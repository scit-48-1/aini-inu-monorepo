package scit.ainiinu.walk.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.service.WalkDiaryService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalkDiaryController.class)
@Import(GlobalExceptionHandler.class)
class WalkDiaryFollowingControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("DIARY-FOLLOWING 계약: Slice 메타(page,size,hasNext)를 반환한다")
    void followingDiarySlice_success() throws Exception {
        // given
        WalkDiaryResponse diary = WalkDiaryResponse.builder()
                .id(30L)
                .memberId(2L)
                .title("팔로잉 일기")
                .content("본문")
                .photoUrls(List.of())
                .walkDate(LocalDate.now())
                .isPublic(true)
                .linkedThreadStatus("NONE")
                .build();

        Slice<WalkDiaryResponse> slice = new SliceImpl<>(List.of(diary), PageRequest.of(0, 1), true);
        given(walkDiaryService.getFollowingDiaries(any(), any())).willReturn(SliceResponse.of(slice));

        // when & then
        mockMvc.perform(get("/api/v1/walk-diaries/following").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(30L))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }
}
