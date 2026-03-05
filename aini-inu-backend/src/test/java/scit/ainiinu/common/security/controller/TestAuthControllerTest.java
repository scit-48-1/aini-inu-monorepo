package scit.ainiinu.common.security.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestAuthController.class)
class TestAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @Test
    @WithMockUser
    @DisplayName("테스트 토큰 발급 API는 access/refresh token을 반환한다")
    void generateTestToken_success() throws Exception {
        given(jwtTokenProvider.generateAccessToken(5L)).willReturn("access-token-5");
        given(jwtTokenProvider.generateRefreshToken(5L)).willReturn("refresh-token-5");

        mockMvc.perform(post("/api/v1/test/auth/token")
                        .with(csrf())
                        .param("memberId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-5"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-5"))
                .andExpect(jsonPath("$.data.memberId").value("5"));
    }
}
