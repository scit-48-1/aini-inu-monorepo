package scit.ainiinu.walk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.service.WalkDiaryService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalkDiaryController.class)
@Import(GlobalExceptionHandler.class)
class WalkDiaryThreadLinkControllerContractTest {

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

    @Test
    @WithMockUser
    @DisplayName("threadId를 포함해 생성할 수 있다")
    void createDiary_withThreadId_success() throws Exception {
        // given
        WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
        request.setThreadId(101L);
        request.setTitle("스레드 연결 일기");
        request.setContent("본문");
        request.setWalkDate(LocalDate.now());
        request.setPhotoUrls(List.of());

        WalkDiaryResponse response = WalkDiaryResponse.builder()
                .id(1L)
                .memberId(1L)
                .threadId(101L)
                .title("스레드 연결 일기")
                .content("본문")
                .photoUrls(List.of())
                .walkDate(LocalDate.now())
                .isPublic(true)
                .linkedThreadStatus("ACTIVE")
                .build();
        given(walkDiaryService.createDiary(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/walk-diaries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threadId").value(101L))
                .andExpect(jsonPath("$.data.linkedThreadStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    @DisplayName("상세 응답에서 삭제된 스레드 상태를 반환한다")
    void detailDiary_deletedThreadStatus_success() throws Exception {
        // given
        WalkDiaryResponse response = WalkDiaryResponse.builder()
                .id(2L)
                .memberId(1L)
                .threadId(202L)
                .title("삭제된 스레드 연결")
                .content("본문")
                .photoUrls(List.of())
                .walkDate(LocalDate.now())
                .isPublic(true)
                .linkedThreadStatus("DELETED")
                .build();
        given(walkDiaryService.getDiary(1L, 2L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/walk-diaries/{diaryId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedThreadStatus").value("DELETED"));
    }
}
