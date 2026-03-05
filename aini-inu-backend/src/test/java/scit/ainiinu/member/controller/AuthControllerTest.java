package scit.ainiinu.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.member.dto.request.AuthLoginRequest;
import scit.ainiinu.member.dto.request.TokenRefreshRequest;
import scit.ainiinu.member.dto.request.TokenRevokeRequest;
import scit.ainiinu.member.dto.response.LoginResponse;
import scit.ainiinu.member.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Nested
    @DisplayName("이메일 로그인")
    class Login {

        @Test
        @WithMockUser
        @DisplayName("유효한 계정 정보로 로그인하면 JWT 토큰을 반환한다")
        void login_withValidCredentials_returnsJwtToken() throws Exception {
            // given
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("Abcd1234!");
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .isNewMember(true)
                    .memberId(1L)
                    .build();

            given(authService.loginWithEmail(any())).willReturn(response);

            // when
            var result = mockMvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.isNewMember").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("이메일 또는 비밀번호가 비어 있으면 400 에러를 반환한다")
        void login_withEmptyCredentials_returnsBadRequest() throws Exception {
            // given
            AuthLoginRequest request = new AuthLoginRequest();
            request.setEmail("");
            request.setPassword("");

            // when
            var result = mockMvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Refresh {

        @Test
        @WithMockUser
        @DisplayName("유효한 리프레시 토큰으로 갱신하면 새 토큰을 반환한다")
        void refresh_withValidRefreshToken_returnsNewTokens() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");
            LoginResponse response = LoginResponse.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .build();

            given(authService.refresh(any())).willReturn(response);

            // when
            var result = mockMvc.perform(post("/api/v1/auth/refresh")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @WithMockUser
        @DisplayName("유효한 리프레시 토큰으로 로그아웃하면 성공한다")
        void logout_withValidRefreshToken_success() throws Exception {
            // given
            TokenRevokeRequest request = new TokenRevokeRequest();
            request.setRefreshToken("valid-refresh-token");
            willDoNothing().given(authService).logout(any(TokenRevokeRequest.class));

            // when
            var result = mockMvc.perform(post("/api/v1/auth/logout")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }
    }
}
