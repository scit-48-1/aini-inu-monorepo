package scit.ainiinu.walk.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.testsupport.IntegrationTestProfile;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;
import scit.ainiinu.walk.repository.WalkingSessionRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:walkingsession-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkingSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WalkingSessionRepository walkingSessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ---------------------------------------------------------------
    // 전체 흐름 통합 테스트
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("전체 흐름 통합 테스트")
    class FullFlow {

        @Test
        @DisplayName("start → heartbeat → active 목록 조회 → stop → 목록에서 사라짐")
        void fullFlow_startHeartbeatActiveStop() throws Exception {
            // given
            Member member = createAndSaveMember("flow@test.com", "흐름유저");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            // 1. start
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.memberId").value(member.getId()));

            // 2. heartbeat
            mockMvc.perform(put("/api/v1/walking-sessions/heartbeat")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 3. active 목록 조회 — 나 포함
            mockMvc.perform(get("/api/v1/walking-sessions/active")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].memberId").value(member.getId()))
                    .andExpect(jsonPath("$.data[0].nickname").value("흐름유저"));

            // 4. stop
            mockMvc.perform(post("/api/v1/walking-sessions/stop")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 5. active 목록 조회 — 비어야 함
            mockMvc.perform(get("/api/v1/walking-sessions/active")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("stop 이후 다시 start 가능")
        void stopThenRestart_success() throws Exception {
            // given
            Member member = createAndSaveMember("restart@test.com", "재시작유저");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            // start → stop
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/walking-sessions/stop")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // start again
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }
    }

    // ---------------------------------------------------------------
    // 에러 케이스
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("에러 케이스")
    class ErrorCases {

        @Test
        @DisplayName("이미 산책중인 유저가 다시 start하면 409")
        void doubleStart_conflict() throws Exception {
            // given
            Member member = createAndSaveMember("double@test.com", "중복유저");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            mockMvc.perform(post("/api/v1/walking-sessions/start")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("WS409_ALREADY_ACTIVE"));
        }

        @Test
        @DisplayName("산책중이 아닌 유저가 heartbeat 보내면 404")
        void heartbeat_withoutStart_notFound() throws Exception {
            // given
            Member member = createAndSaveMember("nohb@test.com", "하트비트없음");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            // when & then
            mockMvc.perform(put("/api/v1/walking-sessions/heartbeat")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("WS404_NOT_FOUND"));
        }

        @Test
        @DisplayName("산책중이 아닌 유저가 stop 보내면 404")
        void stop_withoutStart_notFound() throws Exception {
            // given
            Member member = createAndSaveMember("nostop@test.com", "스톱없음");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            // when & then
            mockMvc.perform(post("/api/v1/walking-sessions/stop")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("WS404_NOT_FOUND"));
        }
    }

    // ---------------------------------------------------------------
    // 내 세션 조회
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("내 세션 조회 API")
    class GetMySession {

        @Test
        @DisplayName("산책중이면 세션 정보를 반환한다")
        void getMySession_active() throws Exception {
            // given
            Member member = createAndSaveMember("mysess@test.com", "내세션유저");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            mockMvc.perform(post("/api/v1/walking-sessions/start")
                            .with(csrf())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/my")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.memberId").value(member.getId()));
        }

        @Test
        @DisplayName("산책중이 아니면 data가 null이다")
        void getMySession_noSession() throws Exception {
            // given
            Member member = createAndSaveMember("nosess@test.com", "세션없음");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/my")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        }
    }

    // ---------------------------------------------------------------
    // 여러 유저 동시 산책
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("여러 유저 동시 산책")
    class MultipleUsers {

        @Test
        @DisplayName("여러 유저가 동시에 산책하면 active 목록에 모두 표시된다")
        void multipleWalkers_allVisible() throws Exception {
            // given
            Member m1 = createAndSaveMember("multi1@test.com", "멀티유저1");
            Member m2 = createAndSaveMember("multi2@test.com", "멀티유저2");
            Member m3 = createAndSaveMember("multi3@test.com", "멀티유저3");
            String t1 = jwtTokenProvider.generateAccessToken(m1.getId());
            String t2 = jwtTokenProvider.generateAccessToken(m2.getId());
            String t3 = jwtTokenProvider.generateAccessToken(m3.getId());

            mockMvc.perform(post("/api/v1/walking-sessions/start")
                    .with(csrf()).header("Authorization", "Bearer " + t1)).andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                    .with(csrf()).header("Authorization", "Bearer " + t2)).andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                    .with(csrf()).header("Authorization", "Bearer " + t3)).andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/active")
                            .header("Authorization", "Bearer " + t1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3));
        }

        @Test
        @DisplayName("한 유저만 stop하면 나머지는 목록에 유지된다")
        void oneStops_othersRemain() throws Exception {
            // given
            Member m1 = createAndSaveMember("remain1@test.com", "유지유저1");
            Member m2 = createAndSaveMember("remain2@test.com", "유지유저2");
            String t1 = jwtTokenProvider.generateAccessToken(m1.getId());
            String t2 = jwtTokenProvider.generateAccessToken(m2.getId());

            mockMvc.perform(post("/api/v1/walking-sessions/start")
                    .with(csrf()).header("Authorization", "Bearer " + t1)).andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/walking-sessions/start")
                    .with(csrf()).header("Authorization", "Bearer " + t2)).andExpect(status().isOk());

            // m1 stops
            mockMvc.perform(post("/api/v1/walking-sessions/stop")
                    .with(csrf()).header("Authorization", "Bearer " + t1)).andExpect(status().isOk());

            // when & then — only m2 in list
            mockMvc.perform(get("/api/v1/walking-sessions/active")
                            .header("Authorization", "Bearer " + t1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].memberId").value(m2.getId()));
        }
    }

    // ---------------------------------------------------------------
    // 스케줄러 로직 검증 (서비스 레이어 직접 호출)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("stale 세션 정리 (스케줄러 로직)")
    class StaleCleanup {

        @Test
        @DisplayName("heartbeat 없이 5분이 지난 세션은 ENDED로 전환된다")
        void staleSession_getsEnded() throws Exception {
            // given
            Member member = createAndSaveMember("stale@test.com", "방치유저");

            WalkingSession session = WalkingSession.create(member.getId());
            ReflectionTestUtils.setField(session, "lastHeartbeatAt",
                    LocalDateTime.now().minusMinutes(10));
            walkingSessionRepository.save(session);

            // when — simulate scheduler logic
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
            var staleSessions = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);
            staleSessions.forEach(WalkingSession::end);
            walkingSessionRepository.flush();

            // then
            WalkingSession found = walkingSessionRepository.findById(session.getId()).orElseThrow();
            assertThat(found.getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
            assertThat(found.getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("heartbeat가 최근인 세션은 정리되지 않는다")
        void freshSession_survives() {
            // given
            Member member = createAndSaveMember("fresh@test.com", "활성유저");

            WalkingSession session = WalkingSession.create(member.getId());
            walkingSessionRepository.save(session);

            // when
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
            var staleSessions = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);
            staleSessions.forEach(WalkingSession::end);

            // then
            WalkingSession found = walkingSessionRepository.findById(session.getId()).orElseThrow();
            assertThat(found.getStatus()).isEqualTo(WalkingSessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("stale과 fresh가 섞여있으면 stale만 정리된다")
        void mixedSessions_onlyStaleEnded() {
            // given
            Member staleUser = createAndSaveMember("mix-stale@test.com", "방치혼합");
            Member freshUser = createAndSaveMember("mix-fresh@test.com", "활성혼합");

            WalkingSession staleSession = WalkingSession.create(staleUser.getId());
            ReflectionTestUtils.setField(staleSession, "lastHeartbeatAt",
                    LocalDateTime.now().minusMinutes(10));
            walkingSessionRepository.save(staleSession);

            WalkingSession freshSession = WalkingSession.create(freshUser.getId());
            walkingSessionRepository.save(freshSession);

            // when
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
            var staleSessions = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);
            staleSessions.forEach(WalkingSession::end);
            walkingSessionRepository.flush();

            // then
            assertThat(walkingSessionRepository.findById(staleSession.getId()).orElseThrow().getStatus())
                    .isEqualTo(WalkingSessionStatus.ENDED);
            assertThat(walkingSessionRepository.findById(freshSession.getId()).orElseThrow().getStatus())
                    .isEqualTo(WalkingSessionStatus.ACTIVE);
        }
    }

    // ---------------------------------------------------------------
    // active 목록 응답 필드 검증
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("응답 필드 검증")
    class ResponseFields {

        @Test
        @DisplayName("active 목록의 각 유저에 mannerTemperature, walkingStartedAt이 포함된다")
        void activeWalkers_hasAllFields() throws Exception {
            // given
            Member member = createAndSaveMember("fields@test.com", "필드유저");
            String token = jwtTokenProvider.generateAccessToken(member.getId());

            mockMvc.perform(post("/api/v1/walking-sessions/start")
                    .with(csrf()).header("Authorization", "Bearer " + token)).andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/api/v1/walking-sessions/active")
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].memberId").value(member.getId()))
                    .andExpect(jsonPath("$.data[0].nickname").value("필드유저"))
                    .andExpect(jsonPath("$.data[0].mannerTemperature").isNumber())
                    .andExpect(jsonPath("$.data[0].walkingStartedAt").isString());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Member createAndSaveMember(String email, String nickname) {
        return memberRepository.save(Member.builder()
                .email(email)
                .nickname(nickname)
                .memberType(MemberType.PET_OWNER)
                .build());
    }
}
