package scit.ainiinu.community.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.community.controller.StoryController;
import scit.ainiinu.community.dto.StoryDiaryItemResponse;
import scit.ainiinu.community.dto.StoryGroupResponse;
import scit.ainiinu.community.service.StoryService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoryController.class)
class StoryListContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoryService storyService;

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
    @DisplayName("스토리 목록 조회는 SliceResponse 계약을 반환한다")
    void getStoriesSliceContract() throws Exception {
        StoryDiaryItemResponse diaryItem = StoryDiaryItemResponse.builder()
                .diaryId(101L)
                .title("아침 산책")
                .content("강아지랑 공원 산책")
                .photoUrls(List.of("https://cdn.example.com/diary-1.jpg"))
                .walkDate(LocalDate.now())
                .createdAt(OffsetDateTime.now())
                .build();

        StoryGroupResponse storyGroup = StoryGroupResponse.builder()
                .memberId(7L)
                .nickname("몽이아빠")
                .profileImageUrl("https://cdn.example.com/profile.jpg")
                .coverImageUrl("https://cdn.example.com/diary-1.jpg")
                .latestCreatedAt(OffsetDateTime.now())
                .diaries(List.of(diaryItem))
                .build();

        SliceResponse<StoryGroupResponse> response = SliceResponse.of(
                new SliceImpl<>(List.of(storyGroup), PageRequest.of(0, 20), false)
        );

        given(storyService.getStories(anyLong(), any())).willReturn(response);

        mockMvc.perform(get("/api/v1/stories")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].memberId").value(7))
                .andExpect(jsonPath("$.data.content[0].diaries[0].diaryId").value(101))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }
}
