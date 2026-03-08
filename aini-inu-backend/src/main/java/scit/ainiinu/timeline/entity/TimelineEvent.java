package scit.ainiinu.timeline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;
import scit.ainiinu.common.event.TimelineEventType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "timeline_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_type", "reference_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelineEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TimelineEventType eventType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(length = 100)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    public TimelineEvent(Long memberId, TimelineEventType eventType, Long referenceId,
                         String title, String summary, String thumbnailUrl, LocalDateTime occurredAt) {
        this.memberId = memberId;
        this.eventType = eventType;
        this.referenceId = referenceId;
        this.title = title;
        this.summary = summary;
        this.thumbnailUrl = thumbnailUrl;
        this.occurredAt = occurredAt;
        this.deleted = false;
    }

    public void markDeleted() {
        this.deleted = true;
    }
}
