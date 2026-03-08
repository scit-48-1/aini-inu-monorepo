package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalkingSessionRepository extends JpaRepository<WalkingSession, Long> {

    Optional<WalkingSession> findByMemberIdAndStatus(Long memberId, WalkingSessionStatus status);

    List<WalkingSession> findAllByStatus(WalkingSessionStatus status);

    List<WalkingSession> findAllByStatusAndLastHeartbeatAtBefore(WalkingSessionStatus status, LocalDateTime cutoff);
}
