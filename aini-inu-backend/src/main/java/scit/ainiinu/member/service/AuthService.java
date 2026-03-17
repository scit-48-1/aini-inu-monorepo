package scit.ainiinu.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.member.dto.request.AuthLoginRequest;
import scit.ainiinu.member.dto.request.MemberSignupRequest;
import scit.ainiinu.member.dto.request.TokenRefreshRequest;
import scit.ainiinu.member.dto.request.TokenRevokeRequest;
import scit.ainiinu.member.dto.response.LoginResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.RefreshToken;
import scit.ainiinu.member.entity.enums.MemberStatus;
import scit.ainiinu.member.exception.MemberErrorCode;
import scit.ainiinu.member.exception.MemberException;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.member.repository.RefreshTokenRepository;

import java.time.LocalDateTime;

/**
 * 인증 관련 비즈니스 로직 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 토큰 갱신 (Refresh Token Rotation 적용)
     */
    @Transactional
    public LoginResponse refresh(TokenRefreshRequest request) {
        // 1. Refresh Token 검증
        String requestToken = request.getRefreshToken();
        Long memberId = jwtTokenProvider.validateAndGetMemberId(requestToken);

        // 2. DB 저장 여부 확인
        RefreshToken savedToken = refreshTokenRepository.findByTokenHash(requestToken)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_TOKEN));

        // 3. 회원 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.getStatus() == MemberStatus.BANNED) {
            throw new MemberException(MemberErrorCode.BANNED_MEMBER);
        }

        // 4. 새 토큰 발급 (RTR: 기존 토큰 삭제 후 새 토큰 발급)
        // 주의: validateAndGetMemberId에서 만료 체크를 하지만, DB의 expiresAt도 이중 체크 권장
        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(savedToken);
            throw new BusinessException(CommonErrorCode.EXPIRED_TOKEN);
        }

        return createLoginResponse(member, false);
    }

    /**
     * 이메일/비밀번호 로그인 처리
     * BCrypt 해싱된 비밀번호와 평문 비밀번호를 모두 지원합니다.
     * 평문 비밀번호로 로그인 성공 시, BCrypt로 자동 마이그레이션합니다.
     */
    @Transactional
    public LoginResponse loginWithEmail(AuthLoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new MemberException(MemberErrorCode.INVALID_CREDENTIALS));

        if (member.getPassword() == null) {
            throw new MemberException(MemberErrorCode.INVALID_CREDENTIALS);
        }

        if (isEncodedPassword(member.getPassword())) {
            // BCrypt 해싱된 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
                throw new MemberException(MemberErrorCode.INVALID_CREDENTIALS);
            }
        } else {
            // 평문 비밀번호 검증 후 BCrypt로 마이그레이션
            if (!member.getPassword().equals(request.getPassword())) {
                throw new MemberException(MemberErrorCode.INVALID_CREDENTIALS);
            }
            member.updatePassword(passwordEncoder.encode(request.getPassword()));
        }

        if (member.getStatus() == MemberStatus.BANNED) {
            throw new MemberException(MemberErrorCode.BANNED_MEMBER);
        }

        return createLoginResponse(member, false);
    }

    /**
     * BCrypt 해싱 여부 판별 ($2a$, $2b$, $2y$ 접두사)
     */
    boolean isEncodedPassword(String password) {
        return password != null && password.startsWith("$2");
    }

    /**
     * 이메일 회원가입 처리
     */
    @Transactional
    public LoginResponse signup(MemberSignupRequest request) {
        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new MemberException(MemberErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        Member newMember = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .memberType(request.getMemberType())
                .build();

        memberRepository.save(newMember);
        return createLoginResponse(newMember, true);
    }

    /**
     * 자체 JWT 토큰 생성 및 응답 객체 조립
     */
    private LoginResponse createLoginResponse(Member member, boolean isNewMember) {
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

        // Refresh Token 저장 (RTR 패턴 적용 가능)
        saveRefreshToken(member, refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L) // 1시간
                .isNewMember(isNewMember)
                .memberId(member.getId())
                .build();
    }

    private void saveRefreshToken(Member member, String tokenValue) {
        // 기존 토큰 삭제 후 저장 (1인 1세션 정책 또는 단순 덮어쓰기)
        refreshTokenRepository.deleteByMember(member);

        RefreshToken refreshToken = RefreshToken.builder()
                .member(member)
                .tokenHash(tokenValue)
                .expiresAt(LocalDateTime.now().plusDays(14))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 로그아웃 처리 (Refresh Token 폐기)
     */
    @Transactional
    public void logout(TokenRevokeRequest request) {
        refreshTokenRepository.findByTokenHash(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

}
