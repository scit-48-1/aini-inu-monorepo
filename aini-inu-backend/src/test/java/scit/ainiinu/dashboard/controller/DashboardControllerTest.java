package scit.ainiinu.dashboard.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.dashboard.dto.response.DashboardSummaryResponse;
import scit.ainiinu.dashboard.service.DashboardService;
import scit.ainiinu.member.dto.response.ActivityStatsResponse;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(
                argThat(parameter -> parameter.hasParameterAnnotation(CurrentMember.class))
        )).willReturn(true);
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("대시보드 summary를 반환한다")
    void getSummary_success() throws Exception {
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .activityStats(ActivityStatsResponse.builder()
                        .windowDays(126)
                        .startDate(LocalDate.of(2025, 11, 8))
                        .endDate(LocalDate.of(2026, 3, 13))
                        .timezone("Asia/Seoul")
                        .totalActivities(12)
                        .points(List.of())
                        .build())
                .hotspots(List.of())
                .threads(List.of())
                .myPets(List.of())
                .recentFriends(List.of())
                .pendingReviews(List.of())
                .build();

        given(dashboardService.getSummary(1L, 37.5, 127.0, 5.0)).willReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0")
                        .param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activityStats.totalActivities").value(12))
                .andExpect(jsonPath("$.data.hotspots").isArray())
                .andExpect(jsonPath("$.data.recentFriends").isArray());
    }
}
