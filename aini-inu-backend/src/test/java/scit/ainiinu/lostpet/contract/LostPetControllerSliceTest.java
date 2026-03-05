package scit.ainiinu.lostpet.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.lostpet.controller.LostPetController;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.dto.LostPetCreateRequest;
import scit.ainiinu.lostpet.dto.LostPetDetailResponse;
import scit.ainiinu.lostpet.dto.LostPetMatchApproveRequest;
import scit.ainiinu.lostpet.dto.LostPetMatchCandidateResponse;
import scit.ainiinu.lostpet.dto.LostPetMatchResponse;
import scit.ainiinu.lostpet.dto.LostPetResponse;
import scit.ainiinu.lostpet.dto.LostPetSummaryResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.service.LostPetAnalyzeService;
import scit.ainiinu.lostpet.service.LostPetMatchApprovalService;
import scit.ainiinu.lostpet.service.LostPetMatchQueryService;
import scit.ainiinu.lostpet.service.LostPetService;

@WebMvcTest(LostPetController.class)
class LostPetControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LostPetService lostPetService;

    @MockitoBean
    private LostPetAnalyzeService lostPetAnalyzeService;

    @MockitoBean
    private LostPetMatchQueryService lostPetMatchQueryService;

    @MockitoBean
    private LostPetMatchApprovalService lostPetMatchApprovalService;

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
    @DisplayName("실종 신고 API")
    class LostPetApi {

        @Test
        @WithMockUser
        @DisplayName("실종 신고 생성이 성공하면 ApiResponse를 반환한다")
        void createLostPet() throws Exception {
            LostPetResponse response = LostPetResponse.builder()
                    .lostPetId(1L)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .build();
            given(lostPetService.create(anyLong(), any(LostPetCreateRequest.class))).willReturn(response);

            String request = """
                    {
                      "petName": "Momo",
                      "breed": "Poodle",
                      "photoUrl": "https://cdn/pets/momo.jpg",
                      "description": "white fur",
                      "lastSeenAt": "2026-02-26T10:00:00",
                      "lastSeenLocation": "Gangnam"
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.lostPetId").value(1L));
        }

        @Test
        @WithMockUser
        @DisplayName("실종 목록 조회는 SliceResponse를 반환한다")
        void listLostPets() throws Exception {
            LostPetSummaryResponse summary = LostPetSummaryResponse.builder()
                    .lostPetId(1L)
                    .petName("Momo")
                    .status("ACTIVE")
                    .lastSeenAt(LocalDateTime.now())
                    .build();
            SliceImpl<LostPetSummaryResponse> slice = new SliceImpl<>(
                    List.of(summary),
                    PageRequest.of(0, 20),
                    false
            );
            given(lostPetService.list(anyLong(), any(), any())).willReturn(slice);

            mockMvc.perform(get("/api/v1/lost-pets")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].lostPetId").value(1L))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @WithMockUser
        @DisplayName("실종 상세 조회가 성공한다")
        void detailLostPet() throws Exception {
            LostPetDetailResponse detail = LostPetDetailResponse.builder()
                    .lostPetId(1L)
                    .ownerId(10L)
                    .petName("Momo")
                    .photoUrl("https://cdn/pets/momo.jpg")
                    .status("ACTIVE")
                    .lastSeenLocation("Gangnam")
                    .lastSeenAt(LocalDateTime.now())
                    .build();
            given(lostPetService.detail(anyLong(), anyLong())).willReturn(detail);

            mockMvc.perform(get("/api/v1/lost-pets/1")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.lostPetId").value(1L));
        }
    }

    @Nested
    @DisplayName("분석/매치 API")
    class AnalyzeAndMatchApi {

        @Test
        @WithMockUser
        @DisplayName("AI 분석 실패 시 500 도메인 에러를 반환한다")
        void analyzeFailWithHttp500() throws Exception {
            given(lostPetAnalyzeService.analyze(anyLong(), any(LostPetAnalyzeRequest.class)))
                    .willThrow(new LostPetException(LostPetErrorCode.L500_AI_ANALYZE_FAILED));

            String request = """
                    {
                      "lostPetId": 1,
                      "imageUrl": "https://cdn/pets/unknown.jpg",
                      "mode": "LOST"
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets/analyze")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("L500_AI_ANALYZE_FAILED"));
        }

        @Test
        @WithMockUser
        @DisplayName("매치 후보 조회가 성공한다")
        void matchCandidates() throws Exception {
            LostPetMatchCandidateResponse candidate = LostPetMatchCandidateResponse.builder()
                    .sessionId(11L)
                    .sightingId(2L)
                    .finderId(22L)
                    .scoreSimilarity(new BigDecimal("0.91000"))
                    .scoreDistance(new BigDecimal("0.60000"))
                    .scoreRecency(new BigDecimal("0.70000"))
                    .scoreTotal(new BigDecimal("0.84700"))
                    .rank(1)
                    .status("CANDIDATE")
                    .build();
            SliceImpl<LostPetMatchCandidateResponse> slice = new SliceImpl<>(
                    List.of(candidate),
                    PageRequest.of(0, 20),
                    false
            );
            given(lostPetMatchQueryService.findCandidates(anyLong(), anyLong(), any(), any())).willReturn(slice);

            mockMvc.perform(get("/api/v1/lost-pets/1/match"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].sessionId").value(11L))
                    .andExpect(jsonPath("$.data.content[0].sightingId").value(2L));
        }

        @Test
        @WithMockUser
        @DisplayName("매치 승인 요청이 성공한다")
        void approveMatch() throws Exception {
            LostPetMatchResponse response = LostPetMatchResponse.builder()
                    .matchId(5L)
                    .status("CHAT_LINKED")
                    .chatRoomId(101L)
                    .build();
            given(lostPetMatchApprovalService.approve(anyLong(), anyLong(), any(LostPetMatchApproveRequest.class), any()))
                    .willReturn(response);

            String request = """
                    {
                      "sessionId": 11,
                      "sightingId": 2
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets/1/match")
                            .with(csrf())
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.chatRoomId").value(101L));
        }

        @Test
        @WithMockUser
        @DisplayName("매치 승인 요청에 sessionId가 없으면 400을 반환한다")
        void approveMatchWithoutSessionId() throws Exception {
            String request = """
                    {
                      "sightingId": 2
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets/1/match")
                            .with(csrf())
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("C002"));
        }
    }
}
