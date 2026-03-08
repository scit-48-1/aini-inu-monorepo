package scit.ainiinu.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ContentCreatedEvent {

    private final Long memberId;
    private final Long referenceId;
    private final TimelineEventType eventType;
    private final String title;
    private final String summary;
    private final String thumbnailUrl;
    private final LocalDateTime occurredAt;

    public static ContentCreatedEvent of(Long memberId, Long referenceId, TimelineEventType eventType,
                                         String title, String summary, String thumbnailUrl) {
        return new ContentCreatedEvent(memberId, referenceId, eventType, title, summary, thumbnailUrl, LocalDateTime.now());
    }
}
