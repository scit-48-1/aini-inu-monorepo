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
import scit.ainiinu.member.dto.request.MemberProfilePatchRequest;
import scit.ainiinu.member.dto.request.MemberSignupRequest;
import scit.ainiinu.member.dto.response.FollowStatusResponse;
import scit.ainiinu.member.dto.response.MemberFollowResponse;
import scit.ainiinu.member.dto.response.MemberResponse;
import scit.ainiinu.member.dto.response.WalkStatsPointResponse;
import scit.ainiinu.member.dto.response.WalkStatsResponse;
import scit.ainiinu.member.entity.enums.Gender;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.service.AuthService;
import scit.ainiinu.member.service.MemberService;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.pet.service.PetService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class  MemberControllerTest {

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

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @WithMockUser
        @DisplayName("유효한 정보로 회원가입하면 토큰을 반환한다")
        void signup_success() throws Exception {
            MemberSignupRequest request = new MemberSignupRequest();
            request.setEmail("new-member@test.com");
            request.setPassword("Abcd1234!");
            request.setNickname("신규회원");
            request.setMemberType(MemberType.NON_PET_OWNER);

            scit.ainiinu.member.dto.response.LoginResponse response = scit.ainiinu.member.dto.response.LoginResponse.builder()
                    .memberId(10L)
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .isNewMember(true)
                    .build();
            given(authService.signup(any(MemberSignupRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/v1/members/signup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.memberId").value(10L));
        }
    }

    @Nested
    @DisplayName("프로필 조회/수정")
    class ProfileReadWrite {

        @Test
        @WithMockUser
        @DisplayName("내 프로필 조회 API를 호출하면 내 프로필을 반환한다")
        void getMyProfile_success() throws Exception {
            MemberResponse response = MemberResponse.builder()
                    .id(1L)
                    .email("me@test.com")
                    .nickname("내닉네임")
                    .personalityTypes(new ArrayList<>())
                    .build();
            given(memberService.getMyProfile(1L)).willReturn(response);

            mockMvc.perform(get("/api/v1/members/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("내닉네임"));
        }

        @Test
        @WithMockUser
        @DisplayName("내 프로필 수정 API를 호출하면 수정된 프로필을 반환한다")
        void patchMyProfile_success() throws Exception {
            MemberProfilePatchRequest request = new MemberProfilePatchRequest();
            request.setNickname("수정닉네임");

            MemberResponse response = MemberResponse.builder()
                    .id(1L)
                    .nickname("수정닉네임")
                    .personalityTypes(new ArrayList<>())
                    .build();
            given(memberService.updateMyProfile(eq(1L), any(MemberProfilePatchRequest.class))).willReturn(response);

            mockMvc.perform(patch("/api/v1/members/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("수정닉네임"));
        }

        @Test
        @WithMockUser
        @DisplayName("타 회원 프로필 조회 API를 호출하면 해당 회원을 반환한다")
        void getMemberProfile_success() throws Exception {
            MemberResponse response = MemberResponse.builder()
                    .id(2L)
                    .nickname("타회원")
                    .personalityTypes(new ArrayList<>())
                    .build();
            given(memberService.getMemberProfile(2L)).willReturn(response);

            mockMvc.perform(get("/api/v1/members/{memberId}", 2L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2L))
                    .andExpect(jsonPath("$.data.nickname").value("타회원"));
        }

        @Test
        @WithMockUser
        @DisplayName("타 회원 반려견 목록 조회 API를 호출하면 목록을 반환한다")
        void getMemberPets_success() throws Exception {
            PetResponse pet = PetResponse.builder()
                    .id(100L)
                    .name("멍멍이")
                    .isMain(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            given(petService.getUserPets(2L)).willReturn(List.of(pet));

            mockMvc.perform(get("/api/v1/members/{memberId}/pets", 2L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(100L))
                    .andExpect(jsonPath("$.data[0].name").value("멍멍이"));
        }
    }

    @Nested
    @DisplayName("팔로우 관계")
    class FollowRelation {

        @Test
        @WithMockUser
        @DisplayName("내 팔로워 목록 조회 API를 호출하면 SliceResponse를 반환한다")
        void getFollowers_success() throws Exception {
            MemberFollowResponse follower = MemberFollowResponse.builder()
                    .id(2L)
                    .nickname("팔로워")
                    .followedAt(LocalDateTime.now())
                    .build();
            given(memberService.getFollowers(eq(1L), any()))
                    .willReturn(new scit.ainiinu.common.response.SliceResponse<>(List.of(follower), 0, 20, true, true, false));

            mockMvc.perform(get("/api/v1/members/me/followers")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(2L))
                    .andExpect(jsonPath("$.data.content[0].nickname").value("팔로워"));
        }

        @Test
        @WithMockUser
        @DisplayName("내 팔로잉 목록 조회 API를 호출하면 SliceResponse를 반환한다")
        void getFollowing_success() throws Exception {
            MemberFollowResponse following = MemberFollowResponse.builder()
                    .id(3L)
                    .nickname("팔로잉")
                    .followedAt(LocalDateTime.now())
                    .build();
            given(memberService.getFollowing(eq(1L), any()))
                    .willReturn(new scit.ainiinu.common.response.SliceResponse<>(List.of(following), 0, 20, true, true, false));

            mockMvc.perform(get("/api/v1/members/me/following")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(3L))
                    .andExpect(jsonPath("$.data.content[0].nickname").value("팔로잉"));
        }

        @Test
        @WithMockUser
        @DisplayName("특정 회원 팔로워 목록 조회 API를 호출하면 SliceResponse를 반환한다")
        void getMemberFollowers_success() throws Exception {
            MemberFollowResponse follower = MemberFollowResponse.builder()
                    .id(3L)
                    .nickname("팔로워A")
                    .followedAt(LocalDateTime.now())
                    .build();
            given(memberService.getFollowers(eq(2L), any()))
                    .willReturn(new scit.ainiinu.common.response.SliceResponse<>(List.of(follower), 0, 20, true, true, false));

            mockMvc.perform(get("/api/v1/members/{memberId}/followers", 2L)
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(3L))
                    .andExpect(jsonPath("$.data.content[0].nickname").value("팔로워A"));
        }

        @Test
        @WithMockUser
        @DisplayName("특정 회원 팔로잉 목록 조회 API를 호출하면 SliceResponse를 반환한다")
        void getMemberFollowing_success() throws Exception {
            MemberFollowResponse following = MemberFollowResponse.builder()
                    .id(4L)
                    .nickname("팔로잉B")
                    .followedAt(LocalDateTime.now())
                    .build();
            given(memberService.getFollowing(eq(2L), any()))
                    .willReturn(new scit.ainiinu.common.response.SliceResponse<>(List.of(following), 0, 20, true, true, false));

            mockMvc.perform(get("/api/v1/members/{memberId}/following", 2L)
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(4L))
                    .andExpect(jsonPath("$.data.content[0].nickname").value("팔로잉B"));
        }

        @Test
        @WithMockUser
        @DisplayName("팔로우 상태 조회 API를 호출하면 팔로우 상태를 반환한다")
        void getFollowStatus_success() throws Exception {
            given(memberService.getFollowStatus(1L, 2L)).willReturn(new FollowStatusResponse(true));

            mockMvc.perform(get("/api/v1/members/me/follows/{targetId}", 2L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isFollowing").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("팔로우 상태 조회 API에서 팔로우하지 않은 경우 false를 반환한다")
        void getFollowStatus_notFollowing() throws Exception {
            given(memberService.getFollowStatus(1L, 3L)).willReturn(new FollowStatusResponse(false));

            mockMvc.perform(get("/api/v1/members/me/follows/{targetId}", 3L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isFollowing").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("팔로우 API를 호출하면 팔로잉 상태를 반환한다")
        void follow_success() throws Exception {
            given(memberService.follow(1L, 2L)).willReturn(new FollowStatusResponse(true));

            mockMvc.perform(post("/api/v1/members/me/follows/{targetId}", 2L)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isFollowing").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("언팔로우 API를 호출하면 언팔로우 상태를 반환한다")
        void unfollow_success() throws Exception {
            given(memberService.unfollow(1L, 2L)).willReturn(new FollowStatusResponse(false));

            mockMvc.perform(delete("/api/v1/members/me/follows/{targetId}", 2L)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isFollowing").value(false));
        }
    }

    @Nested
    @DisplayName("산책 통계")
    class WalkStats {

        @Test
        @WithMockUser
        @DisplayName("산책 통계 조회 API를 호출하면 구조화된 통계를 반환한다")
        void getWalkStats_success() throws Exception {
            WalkStatsResponse response = WalkStatsResponse.builder()
                    .windowDays(126)
                    .startDate(LocalDate.of(2025, 11, 1))
                    .endDate(LocalDate.of(2026, 3, 6))
                    .timezone("Asia/Seoul")
                    .totalWalks(3)
                    .points(List.of(
                            WalkStatsPointResponse.builder().date(LocalDate.of(2026, 3, 4)).count(2).build(),
                            WalkStatsPointResponse.builder().date(LocalDate.of(2026, 3, 5)).count(1).build()
                    ))
                    .build();
            given(memberService.getWalkStats(1L)).willReturn(response);

            mockMvc.perform(get("/api/v1/members/me/stats/walk"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.windowDays").value(126))
                    .andExpect(jsonPath("$.data.totalWalks").value(3))
                    .andExpect(jsonPath("$.data.points[0].count").value(2));
        }
    }
}
