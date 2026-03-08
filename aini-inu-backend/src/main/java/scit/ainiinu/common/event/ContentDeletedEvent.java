package scit.ainiinu.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContentDeletedEvent {

    private final Long memberId;
    private final Long referenceId;
    private final TimelineEventType eventType;

    public static ContentDeletedEvent of(Long memberId, Long referenceId, TimelineEventType eventType) {
        return new ContentDeletedEvent(memberId, referenceId, eventType);
    }
}
