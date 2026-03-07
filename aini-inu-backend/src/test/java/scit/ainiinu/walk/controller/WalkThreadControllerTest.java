package scit.ainiinu.walk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.walk.dto.request.ThreadApplyRequest;
import scit.ainiinu.walk.dto.request.ThreadCreateRequest;
import scit.ainiinu.walk.dto.response.ThreadApplyResponse;
import scit.ainiinu.walk.dto.response.ThreadHotspotResponse;
import scit.ainiinu.walk.dto.response.ThreadMapResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;
import scit.ainiinu.walk.dto.response.ThreadResponse;
import scit.ainiinu.walk.exception.ThreadErrorCode;
import scit.ainiinu.walk.service.WalkThreadService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalkThreadController.class)
@Import(GlobalExceptionHandler.class)
class WalkThreadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalkThreadService walkThreadService;

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

    @Nested
    @DisplayName("스레드 생성 API")
    class CreateThread {

        @Test
        @WithMockUser
        @DisplayName("성공: 유효한 요청이면 스레드를 생성한다")
        void createThread_success() throws Exception {
            // given
            ThreadCreateRequest request = new ThreadCreateRequest();
            request.setTitle("한강 산책 모집");
            request.setDescription("저녁 산책 함께해요");
            request.setWalkDate(LocalDate.now().plusDays(1));
            request.setStartTime(LocalDateTime.now().plusDays(1));
            request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
            request.setChatType("GROUP");
            request.setMaxParticipants(5);
            ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
            location.setPlaceName("서울숲");
            location.setLatitude(37.54);
            location.setLongitude(127.04);
            location.setAddress("성동구");
            request.setLocation(location);
            request.setPetIds(List.of(1L));

            ThreadResponse response = ThreadResponse.builder()
                    .id(1L)
                    .title("한강 산책 모집")
                    .build();
            given(walkThreadService.createThread(anyLong(), any(ThreadCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/threads")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 비애견인은 생성할 수 없다")
        void createThread_nonPetOwner_forbidden() throws Exception {
            // given
            ThreadCreateRequest request = new ThreadCreateRequest();
            request.setTitle("한강 산책 모집");
            request.setDescription("저녁 산책 함께해요");
            request.setWalkDate(LocalDate.now().plusDays(1));
            request.setStartTime(LocalDateTime.now().plusDays(1));
            request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
            request.setChatType("GROUP");
            request.setMaxParticipants(5);
            ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
            location.setPlaceName("서울숲");
            location.setLatitude(37.54);
            location.setLongitude(127.04);
            location.setAddress("성동구");
            request.setLocation(location);
            request.setPetIds(List.of(1L));

            given(walkThreadService.createThread(anyLong(), any(ThreadCreateRequest.class)))
                    .willThrow(new BusinessException(ThreadErrorCode.NON_PET_OWNER_CREATE_FORBIDDEN));

            // when & then
            mockMvc.perform(post("/api/v1/threads")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("T403_NON_PET_OWNER_CREATE_FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("스레드 목록 조회 API")
    class ListThreads {

        @Test
        @WithMockUser
        @DisplayName("성공: SliceResponse로 목록을 조회한다")
        void getThreads_success() throws Exception {
            // given
            ThreadSummaryResponse summary = ThreadSummaryResponse.builder()
                    .id(1L)
                    .title("한강 산책 모집")
                    .build();
            Slice<ThreadSummaryResponse> slice = new SliceImpl<>(List.of(summary), PageRequest.of(0, 20), false);
            given(walkThreadService.getThreads(anyLong(), any(), any(), any(), any(), any(), any())).willReturn(SliceResponse.of(slice));

            // when & then
            mockMvc.perform(get("/api/v1/threads")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1L));
        }
    }

    @Nested
    @DisplayName("스레드 신청 API")
    class ApplyThread {

        @Test
        @WithMockUser
        @DisplayName("성공: 신청하면 JOINED 응답을 반환한다")
        void apply_success() throws Exception {
            // given
            Long threadId = 1L;
            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of(1L));

            ThreadApplyResponse response = ThreadApplyResponse.builder()
                    .threadId(threadId)
                    .chatRoomId(9001L)
                    .applicationStatus("JOINED")
                    .isIdempotentReplay(false)
                    .build();
            given(walkThreadService.applyThread(anyLong(), eq(threadId), any(ThreadApplyRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/threads/{threadId}/apply", threadId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.applicationStatus").value("JOINED"))
                    .andExpect(jsonPath("$.data.isIdempotentReplay").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 정원이 초과되면 T409_CAPACITY_FULL을 반환한다")
        void apply_capacityFull_fail() throws Exception {
            // given
            Long threadId = 1L;
            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of(1L));
            given(walkThreadService.applyThread(anyLong(), eq(threadId), any(ThreadApplyRequest.class)))
                    .willThrow(new BusinessException(ThreadErrorCode.CAPACITY_FULL));

            // when & then
            mockMvc.perform(post("/api/v1/threads/{threadId}/apply", threadId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("T409_CAPACITY_FULL"));
        }
    }

    @Nested
    @DisplayName("스레드 지도/취소/핫스팟 API")
    class MapCancelHotspot {

        @Test
        @WithMockUser
        @DisplayName("성공: 지도 목록 조회를 반환한다")
        void map_success() throws Exception {
            // given
            ThreadMapResponse mapResponse = ThreadMapResponse.builder()
                    .threadId(1L)
                    .title("서울숲 모임")
                    .chatType("GROUP")
                    .currentParticipants(2)
                    .maxParticipants(5)
                    .placeName("서울숲")
                    .build();
            given(walkThreadService.getMapThreads(anyLong(), eq(37.54), eq(127.04), eq(5.0), any(), any()))
                    .willReturn(List.of(mapResponse));

            // when & then
            mockMvc.perform(get("/api/v1/threads/map")
                            .param("latitude", "37.54")
                            .param("longitude", "127.04")
                            .param("radius", "5"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].threadId").value(1L));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 신청 취소를 수행한다")
        void cancelApply_success() throws Exception {
            // given
            Long threadId = 1L;

            // when & then
            mockMvc.perform(delete("/api/v1/threads/{threadId}/apply", threadId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 핫스팟 목록을 조회한다")
        void hotspot_success() throws Exception {
            // given
            given(walkThreadService.getHotspots(24))
                    .willReturn(List.of(new ThreadHotspotResponse("서울숲", 3L)));

            // when & then
            mockMvc.perform(get("/api/v1/threads/hotspot")
                            .param("hours", "24"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].region").value("서울숲"))
                    .andExpect(jsonPath("$.data[0].count").value(3));
        }
    }

    @Nested
    @DisplayName("내 참여 중인 산책 조회 API")
    class GetMyJoinedThreads {

        @Test
        @WithMockUser
        @DisplayName("성공: 참여 중인 산책 목록을 반환한다")
        void getMyJoinedThreads_success() throws Exception {
            // given
            ThreadSummaryResponse summary = ThreadSummaryResponse.builder()
                    .id(10L)
                    .title("서울숲 산책")
                    .isApplied(true)
                    .build();
            given(walkThreadService.getMyJoinedThreads(anyLong())).willReturn(List.of(summary));

            // when & then
            mockMvc.perform(get("/api/v1/threads/my/joined"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].id").value(10L))
                    .andExpect(jsonPath("$.data[0].title").value("서울숲 산책"))
                    .andExpect(jsonPath("$.data[0].isApplied").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 참여 중인 산책이 없으면 빈 배열을 반환한다")
        void getMyJoinedThreads_empty() throws Exception {
            // given
            given(walkThreadService.getMyJoinedThreads(anyLong())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/threads/my/joined"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("스레드 수정 API")
    class PatchThread {

        @Test
        @WithMockUser
        @DisplayName("실패: 작성자가 아닌 경우 403을 반환한다")
        void patch_notOwner_fail() throws Exception {
            // given
            Long threadId = 1L;
            given(walkThreadService.updateThread(anyLong(), eq(threadId), any()))
                    .willThrow(new BusinessException(ThreadErrorCode.THREAD_OWNER_ONLY));

            // when & then
            mockMvc.perform(patch("/api/v1/threads/{threadId}", threadId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("T403_THREAD_OWNER_ONLY"));
        }
    }

    @Nested
    @DisplayName("스레드 상세/삭제 API")
    class DetailDeleteThread {

        @Test
        @WithMockUser
        @DisplayName("성공: 스레드 상세 조회를 반환한다")
        void getThread_success() throws Exception {
            Long threadId = 1L;
            ThreadResponse response = ThreadResponse.builder()
                    .id(threadId)
                    .title("상세 조회 스레드")
                    .description("상세 설명")
                    .build();
            given(walkThreadService.getThread(anyLong(), eq(threadId))).willReturn(response);

            mockMvc.perform(get("/api/v1/threads/{threadId}", threadId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(threadId))
                    .andExpect(jsonPath("$.data.title").value("상세 조회 스레드"));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 스레드 삭제를 수행한다")
        void deleteThread_success() throws Exception {
            Long threadId = 1L;

            mockMvc.perform(delete("/api/v1/threads/{threadId}", threadId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }
    }
}
