package scit.ainiinu.member.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.member.dto.response.MemberPersonalityTypeResponse;
import scit.ainiinu.member.service.MemberPersonalityTypeService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberPersonalityTypeController.class)
class MemberPersonalityTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberPersonalityTypeService memberPersonalityTypeService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }

    @DisplayName("회원 성격 유형 목록 조회 API 호출 시 성공 응답을 반환한다.")
    @Test
    @WithMockUser // 인증된 사용자로 가정
    void getAllPersonalityTypes() throws Exception {
        // given
        List<MemberPersonalityTypeResponse> responses = List.of(
            MemberPersonalityTypeResponse.builder().id(1L).name("동네친구").code("LOCAL_FRIEND").build(),
            MemberPersonalityTypeResponse.builder().id(2L).name("반려견정보공유").code("PET_INFO_SHARING").build()
        );

        given(memberPersonalityTypeService.getAllPersonalityTypes())
            .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/member-personality-types")
                .with(csrf()) // CSRF 토큰 포함
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].code").value("LOCAL_FRIEND"))
            .andExpect(jsonPath("$.data[1].code").value("PET_INFO_SHARING"));
    }
}
