package scit.ainiinu.walk.dto.response;

import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;

import java.time.LocalDateTime;

public record WalkingSessionResponse(
        Long id,
        Long memberId,
        WalkingSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime lastHeartbeatAt
) {
    public static WalkingSessionResponse from(WalkingSession session) {
        return new WalkingSessionResponse(
                session.getId(),
                session.getMemberId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getLastHeartbeatAt()
        );
    }
}
