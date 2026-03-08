package scit.ainiinu.timeline.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.timeline.dto.response.TimelineEventResponse;
import scit.ainiinu.timeline.dto.response.TimelineSettingsResponse;
import scit.ainiinu.timeline.exception.TimelineErrorCode;
import scit.ainiinu.timeline.service.TimelineService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimelineController.class)
@Import(GlobalExceptionHandler.class)
class TimelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimelineService timelineService;

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

    // ---------------------------------------------------------------
    // GET /api/v1/members/{memberId}/timeline
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("타임라인 조회 API")
    class GetTimeline {

        @Test
        @WithMockUser
        @DisplayName("성공: 타임라인 이벤트 목록을 반환한다")
        void getTimeline_success() throws Exception {
            // given
            TimelineEventResponse event = TimelineEventResponse.builder()
                    .id(1L)
                    .eventType(TimelineEventType.POST_CREATED)
                    .referenceId(42L)
                    .title("새 게시글")
                    .summary("게시글 내용 요약")
                    .thumbnailUrl("https://cdn.example.com/img.jpg")
                    .occurredAt(LocalDateTime.of(2026, 3, 8, 10, 0))
                    .build();

            SliceResponse<TimelineEventResponse> response = new SliceResponse<>(
                    List.of(event), 0, 20, true, false, true);

            given(timelineService.getTimeline(eq(1L), eq(2L), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/members/2/timeline")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].eventType").value("POST_CREATED"))
                    .andExpect(jsonPath("$.data.content[0].referenceId").value(42))
                    .andExpect(jsonPath("$.data.content[0].title").value("새 게시글"))
                    .andExpect(jsonPath("$.data.content[0].summary").value("게시글 내용 요약"))
                    .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://cdn.example.com/img.jpg"))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.pageNumber").value(0));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 빈 타임라인은 빈 배열을 반환한다")
        void getTimeline_empty() throws Exception {
            // given
            SliceResponse<TimelineEventResponse> response = new SliceResponse<>(
                    List.of(), 0, 20, true, true, false);
            given(timelineService.getTimeline(eq(1L), eq(2L), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/members/2/timeline"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 비공개 타임라인 조회 시 403")
        void getTimeline_private_forbidden() throws Exception {
            // given
            given(timelineService.getTimeline(eq(1L), eq(2L), any()))
                    .willThrow(new BusinessException(TimelineErrorCode.TIMELINE_NOT_PUBLIC));

            // when & then
            mockMvc.perform(get("/api/v1/members/2/timeline"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("TL403_TIMELINE_NOT_PUBLIC"));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 존재하지 않는 회원 타임라인 조회 시 404")
        void getTimeline_memberNotFound() throws Exception {
            // given
            given(timelineService.getTimeline(eq(1L), eq(999L), any()))
                    .willThrow(new BusinessException(TimelineErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/members/999/timeline"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TL404_MEMBER_NOT_FOUND"));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 페이지네이션 파라미터가 서비스에 전달된다")
        void getTimeline_pagination() throws Exception {
            // given
            SliceResponse<TimelineEventResponse> response = new SliceResponse<>(
                    List.of(), 2, 10, false, true, false);
            given(timelineService.getTimeline(eq(1L), eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/members/1/timeline")
                            .param("page", "2")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pageNumber").value(2))
                    .andExpect(jsonPath("$.data.pageSize").value(10));
        }
    }

    // ---------------------------------------------------------------
    // PATCH /api/v1/members/me/timeline/settings
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("타임라인 설정 변경 API")
    class UpdateSettings {

        @Test
        @WithMockUser
        @DisplayName("성공: 타임라인 공개 설정을 변경한다")
        void updateSettings_success() throws Exception {
            // given
            given(timelineService.updateSettings(anyLong(), any()))
                    .willReturn(new TimelineSettingsResponse(false));

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/timeline/settings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isTimelinePublic\": false}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.timelinePublic").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 공개로 변경한다")
        void updateSettings_toPublic() throws Exception {
            // given
            given(timelineService.updateSettings(anyLong(), any()))
                    .willReturn(new TimelineSettingsResponse(true));

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/timeline/settings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isTimelinePublic\": true}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.timelinePublic").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: isTimelinePublic이 null이면 400")
        void updateSettings_nullField_badRequest() throws Exception {
            // when & then
            mockMvc.perform(patch("/api/v1/members/me/timeline/settings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 존재하지 않는 회원 설정 변경 시 404")
        void updateSettings_memberNotFound() throws Exception {
            // given
            given(timelineService.updateSettings(anyLong(), any()))
                    .willThrow(new BusinessException(TimelineErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/timeline/settings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isTimelinePublic\": false}"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("TL404_MEMBER_NOT_FOUND"));
        }
    }
}
