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
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.service.WalkDiaryService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalkDiaryController.class)
@Import(GlobalExceptionHandler.class)
class WalkDiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalkDiaryService walkDiaryService;

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
    @DisplayName("일기 생성 API")
    class CreateDiary {

        @Test
        @WithMockUser
        @DisplayName("성공: 유효한 요청이면 일기를 생성한다")
        void createDiary_success() throws Exception {
            // given
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setTitle("한강 산책 일기");
            request.setContent("오늘 날씨가 좋았다");
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("https://cdn/1.jpg"));
            request.setIsPublic(true);

            WalkDiaryResponse response = WalkDiaryResponse.builder()
                    .id(1L)
                    .memberId(1L)
                    .title("한강 산책 일기")
                    .content("오늘 날씨가 좋았다")
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            given(walkDiaryService.createDiary(anyLong(), any(WalkDiaryCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/walk-diaries")
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
        @DisplayName("실패: 본문이 300자를 초과하면 400을 반환한다")
        void createDiary_contentTooLong_fail() throws Exception {
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setTitle("한강 산책 일기");
            request.setContent("a".repeat(301));
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("https://cdn/1.jpg"));
            request.setIsPublic(true);

            mockMvc.perform(post("/api/v1/walk-diaries")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("C002"));
        }
    }

    @Nested
    @DisplayName("일기 목록 조회 API")
    class ListDiaries {

        @Test
        @WithMockUser
        @DisplayName("성공: SliceResponse로 목록을 반환한다")
        void list_success() throws Exception {
            // given
            WalkDiaryResponse diary = WalkDiaryResponse.builder()
                    .id(1L)
                    .memberId(1L)
                    .title("한강 산책 일기")
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .build();
            Slice<WalkDiaryResponse> slice = new SliceImpl<>(List.of(diary), PageRequest.of(0, 20), false);
            given(walkDiaryService.getWalkDiaries(anyLong(), any(), any())).willReturn(SliceResponse.of(slice));

            // when & then
            mockMvc.perform(get("/api/v1/walk-diaries")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(1L))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }
    }

    @Nested
    @DisplayName("팔로잉 일기 피드 API")
    class FollowingFeed {

        @Test
        @WithMockUser
        @DisplayName("성공: 팔로잉 공개 일기 목록을 반환한다")
        void following_success() throws Exception {
            // given
            WalkDiaryResponse diary = WalkDiaryResponse.builder()
                    .id(1L)
                    .memberId(2L)
                    .title("팔로잉 일기")
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .build();
            Slice<WalkDiaryResponse> slice = new SliceImpl<>(List.of(diary), PageRequest.of(0, 20), true);
            given(walkDiaryService.getFollowingDiaries(anyLong(), any())).willReturn(SliceResponse.of(slice));

            // when & then
            mockMvc.perform(get("/api/v1/walk-diaries/following")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].memberId").value(2L));
        }
    }

    @Nested
    @DisplayName("일기 수정 API")
    class PatchDiary {

        @Test
        @WithMockUser
        @DisplayName("실패: 작성자가 아니면 403을 반환한다")
        void patch_notOwner_fail() throws Exception {
            // given
            Long diaryId = 1L;
            WalkDiaryPatchRequest request = new WalkDiaryPatchRequest();
            request.setContent("수정");
            given(walkDiaryService.updateDiary(anyLong(), eq(diaryId), any(WalkDiaryPatchRequest.class)))
                    .willThrow(new BusinessException(WalkDiaryErrorCode.DIARY_OWNER_ONLY));

            // when & then
            mockMvc.perform(patch("/api/v1/walk-diaries/{diaryId}", diaryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("WD403_DIARY_OWNER_ONLY"));
        }

        @Test
        @WithMockUser
        @DisplayName("실패: 본문이 300자를 초과하면 400을 반환한다")
        void patch_contentTooLong_fail() throws Exception {
            Long diaryId = 1L;
            WalkDiaryPatchRequest request = new WalkDiaryPatchRequest();
            request.setContent("a".repeat(301));

            mockMvc.perform(patch("/api/v1/walk-diaries/{diaryId}", diaryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("C002"));
        }
    }

    @Nested
    @DisplayName("일기 삭제 API")
    class DeleteDiary {

        @Test
        @WithMockUser
        @DisplayName("성공: 삭제하면 null data를 반환한다")
        void delete_success() throws Exception {
            // given
            Long diaryId = 1L;
            willDoNothing().given(walkDiaryService).deleteDiary(anyLong(), eq(diaryId));

            // when & then
            mockMvc.perform(delete("/api/v1/walk-diaries/{diaryId}", diaryId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
