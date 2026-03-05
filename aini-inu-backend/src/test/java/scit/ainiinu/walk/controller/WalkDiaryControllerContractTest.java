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
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalkDiaryController.class)
@Import(GlobalExceptionHandler.class)
class WalkDiaryControllerContractTest {

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
    @DisplayName("DIARY-CREATE 계약")
    class CreateContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 생성 응답 payload를 반환한다")
        void createDiary_success() throws Exception {
            // given
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setTitle("산책 기록");
            request.setContent("날씨 좋음");
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("https://cdn/1.jpg"));

            WalkDiaryResponse response = WalkDiaryResponse.builder()
                    .id(11L)
                    .memberId(1L)
                    .title("산책 기록")
                    .content("날씨 좋음")
                    .photoUrls(List.of("https://cdn/1.jpg"))
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .linkedThreadStatus("NONE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            given(walkDiaryService.createDiary(anyLong(), any(WalkDiaryCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/walk-diaries")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(11L));
        }
    }

    @Nested
    @DisplayName("DIARY-LIST 계약")
    class ListContract {

        @Test
        @WithMockUser
        @DisplayName("성공: SliceResponse 메타를 반환한다")
        void listDiary_success() throws Exception {
            // given
            WalkDiaryResponse diary = WalkDiaryResponse.builder()
                    .id(11L)
                    .memberId(1L)
                    .title("산책 기록")
                    .content("날씨 좋음")
                    .photoUrls(List.of())
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .linkedThreadStatus("NONE")
                    .build();
            Slice<WalkDiaryResponse> slice = new SliceImpl<>(List.of(diary), PageRequest.of(0, 20), true);
            given(walkDiaryService.getWalkDiaries(anyLong(), any(), any())).willReturn(SliceResponse.of(slice));

            // when & then
            mockMvc.perform(get("/api/v1/walk-diaries").param("page", "0").param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(11L))
                    .andExpect(jsonPath("$.data.hasNext").value(true));
        }
    }

    @Nested
    @DisplayName("DIARY-DETAIL 계약")
    class DetailContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 상세 응답에 linkedThreadStatus를 포함한다")
        void detailDiary_success() throws Exception {
            // given
            WalkDiaryResponse response = WalkDiaryResponse.builder()
                    .id(11L)
                    .memberId(1L)
                    .title("산책 기록")
                    .content("날씨 좋음")
                    .photoUrls(List.of())
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .linkedThreadStatus("ACTIVE")
                    .build();
            given(walkDiaryService.getDiary(1L, 11L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/walk-diaries/{diaryId}", 11L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(11L))
                    .andExpect(jsonPath("$.data.linkedThreadStatus").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("DIARY-UPDATE 계약")
    class UpdateContract {

        @Test
        @WithMockUser
        @DisplayName("성공: patch 결과를 반환한다")
        void patchDiary_success() throws Exception {
            // given
            WalkDiaryPatchRequest request = new WalkDiaryPatchRequest();
            request.setTitle("수정 제목");

            WalkDiaryResponse response = WalkDiaryResponse.builder()
                    .id(11L)
                    .memberId(1L)
                    .title("수정 제목")
                    .content("날씨 좋음")
                    .photoUrls(List.of())
                    .walkDate(LocalDate.now())
                    .isPublic(true)
                    .linkedThreadStatus("NONE")
                    .build();
            given(walkDiaryService.updateDiary(anyLong(), eq(11L), any(WalkDiaryPatchRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/walk-diaries/{diaryId}", 11L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("수정 제목"));
        }
    }

    @Nested
    @DisplayName("DIARY-DELETE 계약")
    class DeleteContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 삭제 응답은 null data를 반환한다")
        void deleteDiary_success() throws Exception {
            // given
            willDoNothing().given(walkDiaryService).deleteDiary(1L, 11L);

            // when & then
            mockMvc.perform(delete("/api/v1/walk-diaries/{diaryId}", 11L).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
