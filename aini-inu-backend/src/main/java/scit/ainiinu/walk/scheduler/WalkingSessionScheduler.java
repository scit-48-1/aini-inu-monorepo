package scit.ainiinu.walk.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;
import scit.ainiinu.walk.repository.WalkingSessionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class WalkingSessionScheduler {

    private static final int HEARTBEAT_TIMEOUT_MINUTES = 5;

    private final WalkingSessionRepository walkingSessionRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void cleanupStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HEARTBEAT_TIMEOUT_MINUTES);
        List<WalkingSession> staleSessions = walkingSessionRepository
                .findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus.ACTIVE, cutoff);

        if (!staleSessions.isEmpty()) {
            staleSessions.forEach(WalkingSession::end);
            log.info("Cleaned up {} stale walking sessions", staleSessions.size());
        }
    }
}
