package scit.ainiinu.timeline.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.timeline.entity.TimelineEvent;

import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    Slice<TimelineEvent> findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(Long memberId, Pageable pageable);

    List<TimelineEvent> findAllByEventTypeAndReferenceId(TimelineEventType eventType, Long referenceId);
}
