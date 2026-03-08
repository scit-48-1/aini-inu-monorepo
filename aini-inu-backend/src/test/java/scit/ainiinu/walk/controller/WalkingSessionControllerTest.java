package scit.ainiinu.walk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.walk.dto.response.WalkingSessionResponse;
import scit.ainiinu.walk.dto.response.WalkingUserResponse;
import scit.ainiinu.walk.entity.WalkingSessionStatus;
import scit.ainiinu.walk.exception.WalkingSessionErrorCode;
import scit.ainiinu.walk.service.WalkingSessionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalkingSessionController.class)
@Import(GlobalExceptionHandler.class)
class WalkingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalkingSessionService walkingSessionService;

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
    // POST /walking-sessions/start
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("산책 시작 API")
    class StartSession {

        @Test
        @WithMockUser
        @DisplayName("성공: 산책 세션을 시작하고 응답을 반환한다")
        void start_success() throws Exception {
            // given
            WalkingSessionResponse response = new WalkingSessionResponse(
                    1L, 1L, WalkingSessionStatus.ACTIVE,
                    LocalDateTime.now(), LocalDateTime.now()
            );
            given(walkingSessionService.startSession(anyLong())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/walking-sessions/start").with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.memberId").value(1L))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 이미 활성 세션이 있으면 409 에러")
        void start_alreadyActive_conflict() throws Exception {
            // given
            given(walkingSessionService.startSession(anyLong()))
                    .willThrow(new BusinessException(WalkingSessionErrorCode.WALKING_SESSION_ALREADY_ACTIVE));

            // when & then
            mockMvc.perform(post("/api/v1/walking-sessions/start").with(csrf()))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("WS409_ALREADY_ACTIVE"));
        }
    }

    // ---------------------------------------------------------------
    // PUT /walking-sessions/heartbeat
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("하트비트 API")
    class HeartbeatApi {

        @Test
        @WithMockUser
        @DisplayName("성공: 하트비트를 갱신한다")
        void heartbeat_success() throws Exception {
            // given
            willDoNothing().given(walkingSessionService).heartbeat(anyLong());

            // when & then
            mockMvc.perform(put("/api/v1/walking-sessions/heartbeat").with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 활성 세션이 없으면 404 에러")
        void heartbeat_notFound() throws Exception {
            // given
            willThrow(new BusinessException(WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND))
                    .given(walkingSessionService).heartbeat(anyLong());

            // when & then
            mockMvc.perform(put("/api/v1/walking-sessions/heartbeat").with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("WS404_NOT_FOUND"));
        }
    }

    // ---------------------------------------------------------------
    // POST /walking-sessions/stop
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("산책 종료 API")
    class StopSession {

        @Test
        @WithMockUser
        @DisplayName("성공: 산책 세션을 종료한다")
        void stop_success() throws Exception {
            // given
            willDoNothing().given(walkingSessionService).stopSession(anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/walking-sessions/stop").with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 활성 세션이 없으면 404 에러")
        void stop_notFound() throws Exception {
            // given
            willThrow(new BusinessException(WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND))
                    .given(walkingSessionService).stopSession(anyLong());

            // when & then
            mockMvc.perform(post("/api/v1/walking-sessions/stop").with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("WS404_NOT_FOUND"));
        }
    }

    // ---------------------------------------------------------------
    // GET /walking-sessions/active
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("산책중인 유저 목록 API")
    class GetActiveWalkers {

        @Test
        @WithMockUser
        @DisplayName("성공: 산책중인 유저 목록을 반환한다")
        void getActiveWalkers_success() throws Exception {
            // given
            WalkingUserResponse user1 = new WalkingUserResponse(
                    1L, "유저1", "img1.jpg", new BigDecimal("5.0"), LocalDateTime.now()
            );
            WalkingUserResponse user2 = new WalkingUserResponse(
                    2L, "유저2", null, new BigDecimal("7.5"), LocalDateTime.now()
            );
            given(walkingSessionService.getActiveWalkers()).willReturn(List.of(user1, user2));

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/active"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].memberId").value(1L))
                    .andExpect(jsonPath("$.data[0].nickname").value("유저1"))
                    .andExpect(jsonPath("$.data[0].profileImageUrl").value("img1.jpg"))
                    .andExpect(jsonPath("$.data[1].memberId").value(2L))
                    .andExpect(jsonPath("$.data[1].profileImageUrl").isEmpty());
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 산책중인 유저가 없으면 빈 배열을 반환한다")
        void getActiveWalkers_empty() throws Exception {
            // given
            given(walkingSessionService.getActiveWalkers()).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/active"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // GET /walking-sessions/my
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("내 산책 세션 조회 API")
    class GetMySession {

        @Test
        @WithMockUser
        @DisplayName("성공: 활성 세션이 있으면 세션 정보를 반환한다")
        void getMySession_active() throws Exception {
            // given
            WalkingSessionResponse response = new WalkingSessionResponse(
                    1L, 1L, WalkingSessionStatus.ACTIVE,
                    LocalDateTime.now(), LocalDateTime.now()
            );
            given(walkingSessionService.getMySession(anyLong())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/my"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 활성 세션이 없으면 data가 null이다")
        void getMySession_noActive() throws Exception {
            // given
            given(walkingSessionService.getMySession(anyLong())).willReturn(null);

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/my"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }
    }
}
