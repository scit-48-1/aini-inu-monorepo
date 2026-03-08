package scit.ainiinu.walk.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:walkingsession;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkingSessionRepositoryTest {

    @Autowired
    private WalkingSessionRepository walkingSessionRepository;

    // ---------------------------------------------------------------
    // findByMemberIdAndStatus
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("findByMemberIdAndStatus")
    class FindByMemberIdAndStatus {

        @Test
        @DisplayName("ACTIVE 상태의 세션을 조회한다")
        void findActive_success() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            walkingSessionRepository.save(session);

            // when
            Optional<WalkingSession> result = walkingSessionRepository
                    .findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMemberId()).isEqualTo(1L);
            assertThat(result.get().getStatus()).isEqualTo(WalkingSessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("ENDED 상태의 세션은 ACTIVE 조회에 걸리지 않는다")
        void findActive_endedNotReturned() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            session.end();
            walkingSessionRepository.save(session);

            // when
            Optional<WalkingSession> result = walkingSessionRepository
                    .findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 유저의 세션은 조회되지 않는다")
        void findActive_differentMember_empty() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            walkingSessionRepository.save(session);

            // when
            Optional<WalkingSession> result = walkingSessionRepository
                    .findByMemberIdAndStatus(999L, WalkingSessionStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("세션이 없으면 빈 Optional을 반환한다")
        void findActive_noSession_empty() {
            // when
            Optional<WalkingSession> result = walkingSessionRepository
                    .findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ENDED로 조회하면 ENDED 세션만 반환한다")
        void findEnded_success() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            session.end();
            walkingSessionRepository.save(session);

            // when
            Optional<WalkingSession> result = walkingSessionRepository
                    .findByMemberIdAndStatus(1L, WalkingSessionStatus.ENDED);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
        }
    }

    // ---------------------------------------------------------------
    // findAllByStatus
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("findAllByStatus")
    class FindAllByStatus {

        @Test
        @DisplayName("ACTIVE 세션만 조회한다")
        void findAllActive_filtersCorrectly() {
            // given
            WalkingSession active1 = WalkingSession.create(1L);
            WalkingSession active2 = WalkingSession.create(2L);
            WalkingSession ended = WalkingSession.create(3L);
            ended.end();

            walkingSessionRepository.saveAll(List.of(active1, active2, ended));

            // when
            List<WalkingSession> activeSessions = walkingSessionRepository
                    .findAllByStatus(WalkingSessionStatus.ACTIVE);

            // then
            assertThat(activeSessions).hasSize(2);
            assertThat(activeSessions).allMatch(s -> s.getStatus() == WalkingSessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("활성 세션이 없으면 빈 리스트를 반환한다")
        void findAllActive_empty() {
            // when
            List<WalkingSession> result = walkingSessionRepository
                    .findAllByStatus(WalkingSessionStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ENDED 세션만 있으면 ACTIVE 조회 시 빈 리스트")
        void findAllActive_onlyEnded_empty() {
            // given
            WalkingSession ended = WalkingSession.create(1L);
            ended.end();
            walkingSessionRepository.save(ended);

            // when
            List<WalkingSession> result = walkingSessionRepository
                    .findAllByStatus(WalkingSessionStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // findAllByStatusAndLastHeartbeatAtBefore
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("findAllByStatusAndLastHeartbeatAtBefore (스케줄러용)")
    class FindStale {

        @Test
        @DisplayName("lastHeartbeatAt이 cutoff 이전인 ACTIVE 세션을 조회한다")
        void findStale_success() {
            // given
            WalkingSession staleSession = WalkingSession.create(1L);
            // Manually set lastHeartbeatAt to 10 minutes ago
            org.springframework.test.util.ReflectionTestUtils.setField(
                    staleSession, "lastHeartbeatAt", LocalDateTime.now().minusMinutes(10)
            );
            walkingSessionRepository.save(staleSession);

            WalkingSession freshSession = WalkingSession.create(2L);
            walkingSessionRepository.save(freshSession);

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

            // when
            List<WalkingSession> staleSessions = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);

            // then
            assertThat(staleSessions).hasSize(1);
            assertThat(staleSessions.get(0).getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("ENDED 세션은 cutoff 이전이어도 조회되지 않는다")
        void findStale_endedNotIncluded() {
            // given
            WalkingSession endedSession = WalkingSession.create(1L);
            org.springframework.test.util.ReflectionTestUtils.setField(
                    endedSession, "lastHeartbeatAt", LocalDateTime.now().minusMinutes(10)
            );
            endedSession.end();
            walkingSessionRepository.save(endedSession);

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

            // when
            List<WalkingSession> result = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("모든 세션이 fresh하면 빈 리스트를 반환한다")
        void findStale_allFresh_empty() {
            // given
            walkingSessionRepository.save(WalkingSession.create(1L));
            walkingSessionRepository.save(WalkingSession.create(2L));

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

            // when
            List<WalkingSession> result = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("여러 stale 세션이 있으면 모두 반환한다")
        void findStale_multiple() {
            // given
            WalkingSession s1 = WalkingSession.create(1L);
            WalkingSession s2 = WalkingSession.create(2L);
            WalkingSession s3 = WalkingSession.create(3L); // fresh

            org.springframework.test.util.ReflectionTestUtils.setField(
                    s1, "lastHeartbeatAt", LocalDateTime.now().minusMinutes(10)
            );
            org.springframework.test.util.ReflectionTestUtils.setField(
                    s2, "lastHeartbeatAt", LocalDateTime.now().minusMinutes(7)
            );

            walkingSessionRepository.saveAll(List.of(s1, s2, s3));

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);

            // when
            List<WalkingSession> result = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("cutoff 경계값: 정확히 cutoff 시점의 세션은 조회되지 않는다 (before)")
        void findStale_exactCutoff_notIncluded() {
            // given
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
            WalkingSession session = WalkingSession.create(1L);
            org.springframework.test.util.ReflectionTestUtils.setField(
                    session, "lastHeartbeatAt", cutoffTime
            );
            walkingSessionRepository.save(session);

            // when — cutoff == lastHeartbeatAt → "before" means strictly before
            List<WalkingSession> result = walkingSessionRepository
                    .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoffTime);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // 영속성 검증
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("엔티티 영속성 검증")
    class Persistence {

        @Test
        @DisplayName("저장 후 조회하면 모든 필드가 유지된다")
        void saveAndRetrieve_allFieldsPreserved() {
            // given
            WalkingSession session = WalkingSession.create(42L);

            // when
            WalkingSession saved = walkingSessionRepository.save(session);
            WalkingSession found = walkingSessionRepository.findById(saved.getId()).orElseThrow();

            // then
            assertThat(found.getMemberId()).isEqualTo(42L);
            assertThat(found.getStatus()).isEqualTo(WalkingSessionStatus.ACTIVE);
            assertThat(found.getStartedAt()).isNotNull();
            assertThat(found.getLastHeartbeatAt()).isNotNull();
            assertThat(found.getEndedAt()).isNull();
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("end() 후 저장하면 ENDED와 endedAt이 영속화된다")
        void endAndSave_persisted() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            walkingSessionRepository.save(session);

            // when
            session.end();
            walkingSessionRepository.saveAndFlush(session);
            WalkingSession found = walkingSessionRepository.findById(session.getId()).orElseThrow();

            // then
            assertThat(found.getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
            assertThat(found.getEndedAt()).isNotNull();
        }
    }
}
