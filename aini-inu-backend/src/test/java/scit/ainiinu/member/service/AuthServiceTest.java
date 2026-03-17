package scit.ainiinu.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.dto.request.AuthLoginRequest;
import scit.ainiinu.member.dto.request.MemberSignupRequest;
import scit.ainiinu.member.dto.request.TokenRefreshRequest;
import scit.ainiinu.member.dto.request.TokenRevokeRequest;
import scit.ainiinu.member.dto.response.LoginResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.RefreshToken;
import scit.ainiinu.member.entity.enums.MemberStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("이메일 로그인")
    class Login {

        @Nested
        @DisplayName("BCrypt 해싱된 비밀번호")
        class WithEncodedPassword {

            @Test
            @DisplayName("해싱된 비밀번호가 일치하면 로그인에 성공한다")
            void login_withCorrectEncodedPassword_succeeds() {
                // given
                AuthLoginRequest request = new AuthLoginRequest();
                request.setEmail("user@example.com");
                request.setPassword("Abcd1234!");

                Member member = Member.builder()
                        .email("user@example.com")
                        .password("$2a$10$hashedPasswordValue")
                        .nickname("기존유저")
                        .build();
                ReflectionTestUtils.setField(member, "id", 1L);

                given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
                given(passwordEncoder.matches("Abcd1234!", "$2a$10$hashedPasswordValue")).willReturn(true);
                given(jwtTokenProvider.generateAccessToken(any())).willReturn("access-token");
                given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh-token");

                // when
                LoginResponse response = authService.loginWithEmail(request);

                // then
                assertThat(response.isNewMember()).isFalse();
                assertThat(response.getAccessToken()).isEqualTo("access-token");
                assertThat(response.getMemberId()).isEqualTo(1L);
                then(passwordEncoder).should(never()).encode(any());
            }

            @Test
            @DisplayName("해싱된 비밀번호가 불일치하면 예외를 던진다")
            void login_withWrongEncodedPassword_throwsException() {
                // given
                AuthLoginRequest request = new AuthLoginRequest();
                request.setEmail("user@example.com");
                request.setPassword("Wrong123!");

                Member member = Member.builder()
                        .email("user@example.com")
                        .password("$2a$10$hashedPasswordValue")
                        .nickname("기존유저")
                        .build();
                ReflectionTestUtils.setField(member, "id", 1L);

                given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
                given(passwordEncoder.matches("Wrong123!", "$2a$10$hashedPasswordValue")).willReturn(false);

                // when & then
                assertThatThrownBy(() -> authService.loginWithEmail(request))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_CREDENTIALS);
            }
        }

        @Nested
        @DisplayName("평문 비밀번호 Lazy Migration")
        class WithPlainTextPassword {

            @Test
            @DisplayName("평문 비밀번호가 일치하면 로그인 성공 후 BCrypt로 마이그레이션한다")
            void login_withCorrectPlainPassword_migratesAndSucceeds() {
                // given
                AuthLoginRequest request = new AuthLoginRequest();
                request.setEmail("user@example.com");
                request.setPassword("Abcd1234!");

                Member member = Member.builder()
                        .email("user@example.com")
                        .password("Abcd1234!")
                        .nickname("기존유저")
                        .build();
                ReflectionTestUtils.setField(member, "id", 1L);

                given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));
                given(passwordEncoder.encode("Abcd1234!")).willReturn("$2a$10$newlyEncodedHash");
                given(jwtTokenProvider.generateAccessToken(any())).willReturn("access-token");
                given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh-token");

                // when
                LoginResponse response = authService.loginWithEmail(request);

                // then
                assertThat(response.isNewMember()).isFalse();
                assertThat(response.getAccessToken()).isEqualTo("access-token");

                // 비밀번호가 BCrypt로 마이그레이션되었는지 확인
                then(passwordEncoder).should().encode("Abcd1234!");
                assertThat(member.getPassword()).isEqualTo("$2a$10$newlyEncodedHash");
            }

            @Test
            @DisplayName("평문 비밀번호가 불일치하면 예외를 던진다")
            void login_withWrongPlainPassword_throwsException() {
                // given
                AuthLoginRequest request = new AuthLoginRequest();
                request.setEmail("user@example.com");
                request.setPassword("Wrong123!");

                Member member = Member.builder()
                        .email("user@example.com")
                        .password("Abcd1234!")
                        .nickname("기존유저")
                        .build();
                ReflectionTestUtils.setField(member, "id", 1L);

                given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(member));

                // when & then
                assertThatThrownBy(() -> authService.loginWithEmail(request))
                        .isInstanceOf(MemberException.class)
                        .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.INVALID_CREDENTIALS);

                // 마이그레이션이 발생하지 않았는지 확인
                then(passwordEncoder).should(never()).encode(any());
            }

            @Test
            @DisplayName("마이그레이션 후 재로그인 시 BCrypt 경로로 검증한다")
            void login_afterMigration_usesBcryptVerification() {
                // given — 첫 번째 로그인: 평문 → 마이그레이션
                AuthLoginRequest firstRequest = new AuthLoginRequest();
                firstRequest.setEmail("user@example.com");
                firstRequest.setPassword("Abcd1234!");

                Member member = Member.builder()
                        .email("user@example.com")
                        .password("Abcd1234!")
                        .nickname("기존유저")
                        .build();
                ReflectionTestUtils.setField(member, "id", 1L);

                given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(member));
                given(passwordEncoder.encode("Abcd1234!")).willReturn("$2a$10$encodedHash");
                given(jwtTokenProvider.generateAccessToken(any())).willReturn("access-token");
                given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh-token");

                authService.loginWithEmail(firstRequest);

                // 마이그레이션 후 member.password가 BCrypt 해시로 변경됨
                assertThat(member.getPassword()).startsWith("$2");

                // given — 두 번째 로그인: BCrypt 경로 사용
                AuthLoginRequest secondRequest = new AuthLoginRequest();
                secondRequest.setEmail("user@example.com");
                secondRequest.setPassword("Abcd1234!");

                given(passwordEncoder.matches("Abcd1234!", "$2a$10$encodedHash")).willReturn(true);

                // when
                LoginResponse response = authService.loginWithEmail(secondRequest);

                // then
                assertThat(response).isNotNull();
                then(passwordEncoder).should().matches("Abcd1234!", "$2a$10$encodedHash");
            }
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

        @Test
        @DisplayName("차단된 회원이 로그인하면 BANNED_MEMBER 예외가 발생한다")
        void login_withBannedMember_throwsException() {
            // given
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("banned@example.com");
            request.setPassword("Abcd1234!");

            Member bannedMember = Member.builder()
                    .email("banned@example.com")
                    .password("$2a$10$hashedPassword")
                    .nickname("차단유저")
                    .build();
            ReflectionTestUtils.setField(bannedMember, "id", 1L);
            ReflectionTestUtils.setField(bannedMember, "status", MemberStatus.BANNED);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.of(bannedMember));
            given(passwordEncoder.matches("Abcd1234!", "$2a$10$hashedPassword")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.loginWithEmail(request))
                    .isInstanceOf(MemberException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.BANNED_MEMBER);
        }
    }

    @Nested
    @DisplayName("isEncodedPassword 판별")
    class IsEncodedPassword {

        @Test
        @DisplayName("$2a$ 접두사를 가진 비밀번호는 BCrypt로 판별한다")
        void bcrypt2a_isEncoded() {
            assertThat(authService.isEncodedPassword("$2a$10$abcdefghijklmnopqrstuv")).isTrue();
        }

        @Test
        @DisplayName("$2b$ 접두사를 가진 비밀번호는 BCrypt로 판별한다")
        void bcrypt2b_isEncoded() {
            assertThat(authService.isEncodedPassword("$2b$10$abcdefghijklmnopqrstuv")).isTrue();
        }

        @Test
        @DisplayName("$2y$ 접두사를 가진 비밀번호는 BCrypt로 판별한다")
        void bcrypt2y_isEncoded() {
            assertThat(authService.isEncodedPassword("$2y$10$abcdefghijklmnopqrstuv")).isTrue();
        }

        @Test
        @DisplayName("평문 비밀번호는 BCrypt가 아닌 것으로 판별한다")
        void plainText_isNotEncoded() {
            assertThat(authService.isEncodedPassword("Abcd1234!")).isFalse();
        }

        @Test
        @DisplayName("null은 BCrypt가 아닌 것으로 판별한다")
        void null_isNotEncoded() {
            assertThat(authService.isEncodedPassword(null)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열은 BCrypt가 아닌 것으로 판별한다")
        void empty_isNotEncoded() {
            assertThat(authService.isEncodedPassword("")).isFalse();
        }

        @Test
        @DisplayName("$2로 시작하지만 BCrypt가 아닌 문자열도 해싱된 것으로 판별한다")
        void dollarSign2_isEncoded() {
            // $2로 시작하는 평문은 현실적으로 거의 없으므로 안전하게 해싱으로 취급
            assertThat(authService.isEncodedPassword("$2something")).isTrue();
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
        @DisplayName("회원가입 시 비밀번호를 BCrypt로 해싱하여 저장한다")
        void signup_encodesPassword() {
            // given
            MemberSignupRequest request = new MemberSignupRequest();
            request.setEmail("new@test.com");
            request.setPassword("Abcd1234!");
            request.setNickname("신규유저");
            request.setMemberType(MemberType.NON_PET_OWNER);

            given(memberRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
            given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
            given(passwordEncoder.encode("Abcd1234!")).willReturn("$2a$10$encodedPassword");
            given(jwtTokenProvider.generateAccessToken(any())).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh-token");

            // when
            LoginResponse response = authService.signup(request);

            // then
            assertThat(response.isNewMember()).isTrue();
            then(passwordEncoder).should().encode("Abcd1234!");

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            then(memberRepository).should().save(memberCaptor.capture());
            assertThat(memberCaptor.getValue().getPassword()).isEqualTo("$2a$10$encodedPassword");
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
