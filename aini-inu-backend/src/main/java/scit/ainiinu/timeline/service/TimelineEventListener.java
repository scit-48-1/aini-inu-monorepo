package scit.ainiinu.timeline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.ContentDeletedEvent;
import scit.ainiinu.timeline.entity.TimelineEvent;
import scit.ainiinu.timeline.repository.TimelineEventRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TimelineEventListener {

    private final TimelineEventRepository timelineEventRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onContentCreated(ContentCreatedEvent event) {
        TimelineEvent timelineEvent = TimelineEvent.builder()
                .memberId(event.getMemberId())
                .eventType(event.getEventType())
                .referenceId(event.getReferenceId())
                .title(event.getTitle())
                .summary(event.getSummary())
                .thumbnailUrl(event.getThumbnailUrl())
                .occurredAt(event.getOccurredAt())
                .build();
        timelineEventRepository.save(timelineEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onContentDeleted(ContentDeletedEvent event) {
        List<TimelineEvent> events = timelineEventRepository
                .findAllByEventTypeAndReferenceId(event.getEventType(), event.getReferenceId());
        for (TimelineEvent timelineEvent : events) {
            timelineEvent.markDeleted();
        }
    }
}
