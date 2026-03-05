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
import scit.ainiinu.member.dto.request.TokenRefreshRequest;
import scit.ainiinu.member.dto.response.LoginResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.RefreshToken;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.member.repository.RefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
}
