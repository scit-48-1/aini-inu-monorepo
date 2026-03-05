package scit.ainiinu.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.core.MethodParameter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.member.dto.request.MemberCreateRequest;
import scit.ainiinu.member.dto.response.MemberResponse;
import scit.ainiinu.member.entity.enums.Gender;
import scit.ainiinu.member.service.AuthService;
import scit.ainiinu.member.service.MemberService;
import scit.ainiinu.pet.service.PetService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(any())).willAnswer(invocation -> {
            MethodParameter parameter = invocation.getArgument(0);
            return parameter.hasParameterAnnotation(CurrentMember.class);
        });
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Nested
    @DisplayName("프로필 생성")
    class CreateProfile {

        @Test
        @WithMockUser
        @DisplayName("유효한 정보로 프로필을 생성하면 성공한다")
        void createProfile_withValidInfo_succeeds() throws Exception {
            // given
            MemberCreateRequest request = new MemberCreateRequest();
            request.setNickname("건홍이네");
            request.setAge(29);
            request.setGender(Gender.MALE);

            MemberResponse response = MemberResponse.builder()
                    .id(1L)
                    .nickname("건홍이네")
                    .age(29)
                    .gender(Gender.MALE)
                    .personalityTypes(new ArrayList<>())
                    .build();

            given(memberService.createProfile(eq(1L), any(MemberCreateRequest.class))).willReturn(response);

            // when
            var result = mockMvc.perform(post("/api/v1/members/profile")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("건홍이네"));
        }

        @Test
        @WithMockUser
        @DisplayName("유효하지 않은 닉네임으로 생성하면 에러를 반환한다")
        void createProfile_withInvalidNickname_returnsBadRequest() throws Exception {
            // given
            MemberCreateRequest request = new MemberCreateRequest();
            request.setNickname("");

            // when
            var result = mockMvc.perform(post("/api/v1/members/profile")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @WithMockUser
    @DisplayName("회원 검색 API를 호출하면 SliceResponse를 반환한다")
    void searchMembers_returnsSliceResponse() throws Exception {
        MemberResponse member = MemberResponse.builder()
                .id(2L)
                .nickname("이웃멍멍")
                .personalityTypes(new ArrayList<>())
                .build();

        given(memberService.searchMembers(eq(1L), eq("이웃"), any()))
                .willReturn(new scit.ainiinu.common.response.SliceResponse<>(List.of(member), 0, 20, true, true, false));

        mockMvc.perform(get("/api/v1/members/search")
                        .param("q", "이웃"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].nickname").value("이웃멍멍"));
    }
}
