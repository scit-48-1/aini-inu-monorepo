package scit.ainiinu.walk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "walking_session")
public class WalkingSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalkingSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    public static WalkingSession create(Long memberId) {
        WalkingSession session = new WalkingSession();
        session.memberId = memberId;
        session.status = WalkingSessionStatus.ACTIVE;
        session.startedAt = LocalDateTime.now();
        session.lastHeartbeatAt = LocalDateTime.now();
        return session;
    }

    public void refreshHeartbeat() {
        this.lastHeartbeatAt = LocalDateTime.now();
    }

    public void end() {
        this.status = WalkingSessionStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }
}
