package scit.ainiinu.timeline.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.timeline.entity.TimelineEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    Slice<TimelineEvent> findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(Long memberId, Pageable pageable);

    List<TimelineEvent> findAllByEventTypeAndReferenceId(TimelineEventType eventType, Long referenceId);

    @Query("""
            select cast(te.occurredAt as localdate) as activityDate, count(te) as activityCount
            from TimelineEvent te
            where te.memberId = :memberId
              and te.deleted = false
              and te.occurredAt between :start and :end
            group by cast(te.occurredAt as localdate)
            """)
    List<ActivityDailyCountProjection> countDailyActivities(
            @Param("memberId") Long memberId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
