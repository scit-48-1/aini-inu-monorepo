package scit.ainiinu.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.dto.request.AuthLoginRequest;
import scit.ainiinu.member.dto.request.MemberSignupRequest;
import scit.ainiinu.member.dto.request.TokenRefreshRequest;
import scit.ainiinu.member.dto.request.TokenRevokeRequest;
import scit.ainiinu.member.dto.response.LoginResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.RefreshToken;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.exception.MemberErrorCode;
import scit.ainiinu.member.exception.MemberException;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.member.repository.RefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("이메일 로그인")
    class Login {

        @Test
        @DisplayName("기존 회원이 로그인하면 isNewMember는 false를 반환한다")
        void login_withExistingMember_returnsIsNewMemberFalse() {
            // given
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("Abcd1234!");

            Member existingMember = Member.builder()
                    .email("user@example.com")
                    .password("Abcd1234!")
                    .nickname("기존유저")
                    .build();
            ReflectionTestUtils.setField(existingMember, "id", 1L);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(existingMember));
            given(jwtTokenProvider.generateAccessToken(any())).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh-token");

            // when
            LoginResponse response = authService.loginWithEmail(request);

            // then
            assertThat(response.isNewMember()).isFalse();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getMemberId()).isEqualTo(1L);

            then(refreshTokenRepository).should().deleteByMember(any());
            then(refreshTokenRepository).should().save(any());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외를 던진다")
        void login_withInvalidPassword_throwsException() {
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("Wrong123!");

            Member existingMember = Member.builder()
                    .email("user@example.com")
                    .password("Abcd1234!")
                    .nickname("기존유저")
                    .build();
            ReflectionTestUtils.setField(existingMember, "id", 1L);
            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(existingMember));

            assertThatThrownBy(() -> authService.loginWithEmail(request))
                    .isInstanceOf(MemberException.class);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 INVALID_CREDENTIALS 예외가 발생한다")
        void login_withNonExistentEmail_throwsException() {
            // given
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("unknown@example.com");
            request.setPassword("Abcd1234!");

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.loginWithEmail(request))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("비밀번호가 null인 회원이 로그인하면 INVALID_CREDENTIALS 예외가 발생한다")
        void login_withNullPassword_throwsException() {
            // given
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("Abcd1234!");

            Member memberWithNullPw = Member.builder()
                    .email("user@example.com")
                    .password(null)
                    .nickname("유저")
                    .build();
            ReflectionTestUtils.setField(memberWithNullPw, "id", 1L);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(memberWithNullPw));

            // when & then
            assertThatThrownBy(() -> authService.loginWithEmail(request))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Refresh {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 갱신하면 새 토큰을 발급한다")
        void refresh_withValidToken_issuesNewTokens() {
            // given
            String oldRefreshToken = "old-refresh-token";
            TokenRefreshRequest request = new TokenRefreshRequest(oldRefreshToken);
            Long memberId = 1L;

            Member member = Member.builder().build();
            ReflectionTestUtils.setField(member, "id", memberId);

            RefreshToken storedToken = RefreshToken.builder()
                    .member(member)
                    .tokenHash(oldRefreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(jwtTokenProvider.validateAndGetMemberId(oldRefreshToken)).willReturn(memberId);
            given(refreshTokenRepository.findByTokenHash(oldRefreshToken)).willReturn(Optional.of(storedToken));
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(jwtTokenProvider.generateAccessToken(memberId)).willReturn("new-access-token");
            given(jwtTokenProvider.generateRefreshToken(memberId)).willReturn("new-refresh-token");

            // when
            LoginResponse response = authService.refresh(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

            then(refreshTokenRepository).should().deleteByMember(member);
            then(refreshTokenRepository).should().save(any());
        }
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("회원가입 시 비밀번호를 포함해 회원을 저장한다")
        void signup_storesPassword() {
            MemberSignupRequest request = new MemberSignupRequest();
            request.setEmail("new@test.com");
            request.setPassword("Abcd1234!");
            request.setNickname("신규유저");
            request.setMemberType(MemberType.NON_PET_OWNER);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
            given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
            given(jwtTokenProvider.generateAccessToken(any())).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh-token");

            LoginResponse response = authService.signup(request);

            assertThat(response.isNewMember()).isTrue();
            then(memberRepository).should().save(any(Member.class));
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 회원가입하면 예외가 발생한다")
        void signup_withDuplicateEmail_throwsException() {
            // given
            MemberSignupRequest request = new MemberSignupRequest();
            request.setEmail("existing@test.com");
            request.setPassword("Abcd1234!");
            request.setNickname("새유저");
            request.setMemberType(MemberType.NON_PET_OWNER);

            Member existingMember = Member.builder()
                    .email("existing@test.com")
                    .nickname("기존유저")
                    .build();

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(existingMember));

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.DUPLICATE_EMAIL);
        }

        @Test
        @DisplayName("이미 존재하는 닉네임으로 회원가입하면 예외가 발생한다")
        void signup_withDuplicateNickname_throwsException() {
            // given
            MemberSignupRequest request = new MemberSignupRequest();
            request.setEmail("new@test.com");
            request.setPassword("Abcd1234!");
            request.setNickname("중복닉네임");
            request.setMemberType(MemberType.NON_PET_OWNER);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
            given(memberRepository.existsByNickname(request.getNickname())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.DUPLICATE_NICKNAME);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("리프레시 토큰이 존재하면 삭제한다")
        void logout_withToken_deletesToken() {
            Member member = Member.builder()
                    .email("user@test.com")
                    .nickname("유저")
                    .build();
            ReflectionTestUtils.setField(member, "id", 1L);
            RefreshToken refreshToken = RefreshToken.builder()
                    .member(member)
                    .tokenHash("refresh-token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            given(refreshTokenRepository.findByTokenHash("refresh-token")).willReturn(Optional.of(refreshToken));

            TokenRevokeRequest request = new TokenRevokeRequest();
            request.setRefreshToken("refresh-token");
            authService.logout(request);

            then(refreshTokenRepository).should().delete(refreshToken);
        }
    }
}
