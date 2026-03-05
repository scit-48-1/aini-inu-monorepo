package scit.ainiinu.lostpet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.lostpet.integration.ai.LostPetAiCandidate;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClient;
import scit.ainiinu.lostpet.integration.ai.LostPetAiResult;
import scit.ainiinu.lostpet.integration.chat.ChatRoomDirectClient;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.testsupport.IntegrationTestProfile;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class LostPetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LostPetReportRepository lostPetReportRepository;

    @Autowired
    private LostPetSearchSessionRepository lostPetSearchSessionRepository;

    @MockitoBean
    private LostPetAiClient lostPetAiClient;

    @MockitoBean
    private ChatRoomDirectClient chatRoomDirectClient;

    @Nested
    @DisplayName("실종 신고 플로우")
    class LostReportFlow {

        @Test
        @DisplayName("실종 신고 생성 후 목록/상세 조회가 가능하다")
        void createListDetail() throws Exception {
            Long memberId = 10L;
            String token = jwtTokenProvider.generateAccessToken(memberId);

            String createRequest = """
                    {
                      "petName": "Momo",
                      "breed": "Poodle",
                      "photoUrl": "https://cdn/momo.jpg",
                      "description": "desc",
                      "lastSeenAt": "2026-02-26T10:00:00",
                      "lastSeenLocation": "Gangnam"
                    }
                    """;

            MvcResult createResult = mockMvc.perform(post("/api/v1/lost-pets")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lostPetId").exists())
                    .andReturn();

            mockMvc.perform(get("/api/v1/lost-pets")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].petName").value("Momo"));

            JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
            long lostPetId = createNode.path("data").path("lostPetId").asLong();

            mockMvc.perform(get("/api/v1/lost-pets/" + lostPetId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lostPetId").value(lostPetId));
        }
    }

    @Nested
    @DisplayName("인증/권한 정책")
    class AuthPolicyFlow {

        @Test
        @DisplayName("실종 신고 생성 API는 인증이 없으면 401을 반환한다")
        void lostPetCreateRequiresAuth() throws Exception {
            String createRequest = """
                    {
                      "petName": "Momo",
                      "breed": "Poodle",
                      "photoUrl": "https://cdn/momo.jpg",
                      "description": "desc",
                      "lastSeenAt": "2026-02-26T10:00:00",
                      "lastSeenLocation": "Gangnam"
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("실종 목록 조회 API는 인증이 없으면 401을 반환한다")
        void lostPetListRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/lost-pets"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("실종 상세 조회 API는 인증이 없으면 401을 반환한다")
        void lostPetDetailRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/lost-pets/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("매치 승인 API는 인증이 없으면 401을 반환한다")
        void matchApproveRequiresAuth() throws Exception {
            String approveRequest = """
                    {
                      "sessionId": 1,
                      "sightingId": 2
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets/1/match")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approveRequest))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("제보 등록 API는 인증이 없으면 401을 반환한다")
        void sightingCreateRequiresAuth() throws Exception {
            String request = """
                    {
                      "photoUrl": "https://cdn/sightings/1.jpg",
                      "foundAt": "2026-02-26T11:10:00",
                      "foundLocation": "Yeoksam",
                      "memo": "brown collar"
                    }
                    """;

            mockMvc.perform(post("/api/v1/sightings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("타인 실종 상세 조회는 403을 반환한다")
        void lostPetDetailForbiddenForNonOwner() throws Exception {
            String ownerToken = jwtTokenProvider.generateAccessToken(10L);
            String otherToken = jwtTokenProvider.generateAccessToken(99L);
            long lostPetId = createLostPet(ownerToken);

            mockMvc.perform(get("/api/v1/lost-pets/" + lostPetId)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("L403"));
        }

        @Test
        @DisplayName("타인 실종 분석 요청은 403을 반환한다")
        void analyzeForbiddenForNonOwner() throws Exception {
            String ownerToken = jwtTokenProvider.generateAccessToken(10L);
            String otherToken = jwtTokenProvider.generateAccessToken(99L);
            long lostPetId = createLostPet(ownerToken);
            String analyzeRequest = """
                    {
                      "lostPetId": %d,
                      "imageUrl": "https://cdn/unknown.jpg",
                      "mode": "LOST"
                    }
                    """.formatted(lostPetId);

            mockMvc.perform(post("/api/v1/lost-pets/analyze")
                            .with(csrf())
                            .header("Authorization", "Bearer " + otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(analyzeRequest))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("L403"));
        }

        @Test
        @DisplayName("타인 매치 후보 조회는 403을 반환한다")
        void matchForbiddenForNonOwner() throws Exception {
            String ownerToken = jwtTokenProvider.generateAccessToken(10L);
            String otherToken = jwtTokenProvider.generateAccessToken(99L);
            long lostPetId = createLostPet(ownerToken);

            mockMvc.perform(get("/api/v1/lost-pets/" + lostPetId + "/match")
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("L403"));
        }

        @Test
        @DisplayName("타인 매치 승인 요청은 403을 반환한다")
        void matchApproveForbiddenForNonOwner() throws Exception {
            String ownerToken = jwtTokenProvider.generateAccessToken(10L);
            String otherToken = jwtTokenProvider.generateAccessToken(99L);
            long lostPetId = createLostPet(ownerToken);
            String approveRequest = """
                    {
                      "sessionId": 1,
                      "sightingId": 2
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets/" + lostPetId + "/match")
                            .with(csrf())
                            .header("Authorization", "Bearer " + otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approveRequest))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("L403"));
        }
    }

    @Nested
    @DisplayName("AI/매치 플로우")
    class AnalyzeAndMatchFlow {

        @Test
        @DisplayName("분석 API는 인증이 없으면 401을 반환한다")
        void analyzeRequiresAuth() throws Exception {
            String analyzeRequest = """
                    {
                      "lostPetId": 1,
                      "imageUrl": "https://cdn/unknown.jpg",
                      "mode": "LOST"
                    }
                    """;

            mockMvc.perform(post("/api/v1/lost-pets/analyze")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(analyzeRequest))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("매치 후보 조회 API는 인증이 없으면 401을 반환한다")
        void matchRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/lost-pets/1/match"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("C101"));
        }

        @Test
        @DisplayName("AI 예외 발생 시 500을 반환하고 세션을 생성하지 않는다")
        void analyzeFailWithoutSessionCreation() throws Exception {
            Long ownerId = 10L;
            String ownerToken = jwtTokenProvider.generateAccessToken(ownerId);
            long lostPetId = createLostPet(ownerToken);

            given(lostPetAiClient.analyze(any())).willThrow(new RuntimeException("timeout"));
            long sessionCountBefore = lostPetSearchSessionRepository.count();

            String analyzeRequest = """
                    {
                      "lostPetId": %d,
                      "imageUrl": "https://cdn/unknown.jpg",
                      "mode": "LOST"
                    }
                    """.formatted(lostPetId);

            mockMvc.perform(post("/api/v1/lost-pets/analyze")
                            .with(csrf())
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(analyzeRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("L500_AI_ANALYZE_FAILED"));
            assertThat(lostPetSearchSessionRepository.count()).isEqualTo(sessionCountBefore);
        }

        @Test
        @DisplayName("분석-후보조회-승인-채팅연결까지 세션 기반으로 동작한다")
        void analyzeMatchApproveFlow() throws Exception {
            Long ownerId = 10L;
            Long finderId = 22L;
            String ownerToken = jwtTokenProvider.generateAccessToken(ownerId);
            String finderToken = jwtTokenProvider.generateAccessToken(finderId);

            long lostPetId = createLostPet(ownerToken);
            long sightingId = createSighting(finderToken);

            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult(
                            "ok",
                            List.of(new LostPetAiCandidate(sightingId, finderId, new BigDecimal("0.91000")))
                    ));
            given(chatRoomDirectClient.createDirectRoom(any(), any())).willReturn(777L);

            String analyzeRequest = """
                    {
                      "lostPetId": %d,
                      "imageUrl": "https://cdn/unknown.jpg",
                      "mode": "LOST"
                    }
                    """.formatted(lostPetId);

            MvcResult analyzeResult = mockMvc.perform(post("/api/v1/lost-pets/analyze")
                            .with(csrf())
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(analyzeRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.summary").value("ok"))
                    .andExpect(jsonPath("$.data.candidates[0].sightingId").value(sightingId))
                    .andReturn();

            Long sessionId = extractLongDataField(analyzeResult, "sessionId");

            mockMvc.perform(get("/api/v1/lost-pets/" + lostPetId + "/match?sessionId=" + sessionId)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].sessionId").value(sessionId))
                    .andExpect(jsonPath("$.data.content[0].sightingId").value(sightingId));

            String approveRequest = """
                    {
                      "sessionId": %d,
                      "sightingId": %d
                    }
                    """.formatted(sessionId, sightingId);

            mockMvc.perform(post("/api/v1/lost-pets/" + lostPetId + "/match")
                            .with(csrf())
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approveRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CHAT_LINKED"))
                    .andExpect(jsonPath("$.data.chatRoomId").value(777L));
        }

        @Test
        @DisplayName("만료된 세션으로 후보 조회 시 410을 반환한다")
        void matchExpiredSessionResponse() throws Exception {
            Long ownerId = 10L;
            String ownerToken = jwtTokenProvider.generateAccessToken(ownerId);
            long lostPetId = createLostPet(ownerToken);

            LostPetReport report = lostPetReportRepository.findById(lostPetId).orElseThrow();
            LostPetSearchSession expiredSession = lostPetSearchSessionRepository.save(
                    LostPetSearchSession.create(
                            ownerId,
                            report,
                            "LOST",
                            "https://cdn/expired.jpg",
                            null,
                            LocalDateTime.now().minusMinutes(1)
                    )
            );

            mockMvc.perform(get("/api/v1/lost-pets/" + lostPetId + "/match?sessionId=" + expiredSession.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.errorCode").value("L410_SEARCH_SESSION_EXPIRED"));
        }

        @Test
        @DisplayName("세션에 없는 후보 승인 요청 시 409를 반환한다")
        void approveInvalidSessionCandidate() throws Exception {
            Long ownerId = 10L;
            Long finderId = 22L;
            String ownerToken = jwtTokenProvider.generateAccessToken(ownerId);
            String finderToken = jwtTokenProvider.generateAccessToken(finderId);
            long lostPetId = createLostPet(ownerToken);
            long sightingId = createSighting(finderToken);

            LostPetReport report = lostPetReportRepository.findById(lostPetId).orElseThrow();
            LostPetSearchSession session = lostPetSearchSessionRepository.save(
                    LostPetSearchSession.create(
                            ownerId,
                            report,
                            "LOST",
                            "https://cdn/query.jpg",
                            null,
                            LocalDateTime.now().plusHours(24)
                    )
            );

            String approveRequest = """
                    {
                      "sessionId": %d,
                      "sightingId": %d
                    }
                    """.formatted(session.getId(), sightingId);

            mockMvc.perform(post("/api/v1/lost-pets/" + lostPetId + "/match")
                            .with(csrf())
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approveRequest))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("L409_SEARCH_CANDIDATE_INVALID"));
        }
    }

    private long createLostPet(String token) throws Exception {
        String createRequest = """
                {
                  "petName": "Momo",
                  "breed": "Poodle",
                  "photoUrl": "https://cdn/momo.jpg",
                  "description": "desc",
                  "lastSeenAt": "2026-02-26T10:00:00",
                  "lastSeenLocation": "Gangnam"
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/lost-pets")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isOk())
                .andReturn();
        return extractLongDataField(result, "lostPetId");
    }

    private long createSighting(String token) throws Exception {
        String request = """
                {
                  "photoUrl": "https://cdn/sightings/1.jpg",
                  "foundAt": "2026-02-26T11:10:00",
                  "foundLocation": "Yeoksam",
                  "memo": "brown collar"
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/sightings")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andReturn();
        return extractLongDataField(result, "sightingId");
    }

    private Long extractLongDataField(MvcResult result, String fieldName) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.path("data").path(fieldName).asLong();
    }
}
