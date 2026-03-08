package scit.ainiinu.timeline.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.timeline.entity.TimelineEvent;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:timeline;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class TimelineEventRepositoryTest {

    @Autowired
    private TimelineEventRepository timelineEventRepository;

    // ---------------------------------------------------------------
    // findByMemberIdAndDeletedFalseOrderByOccurredAtDesc
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("findByMemberIdAndDeletedFalseOrderByOccurredAtDesc")
    class FindByMemberTimeline {

        @Test
        @DisplayName("해당 회원의 삭제되지 않은 이벤트를 시간 역순으로 반환한다")
        void success_orderedByOccurredAtDesc() {
            // given
            LocalDateTime now = LocalDateTime.now();
            TimelineEvent older = saveEvent(1L, TimelineEventType.POST_CREATED, 10L, now.minusHours(2));
            TimelineEvent newer = saveEvent(1L, TimelineEventType.WALK_THREAD_CREATED, 11L, now.minusHours(1));
            TimelineEvent newest = saveEvent(1L, TimelineEventType.WALKING_SESSION_STARTED, 12L, now);

            Pageable pageable = PageRequest.of(0, 20);

            // when
            Slice<TimelineEvent> result = timelineEventRepository
                    .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(1L, pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).getReferenceId()).isEqualTo(12L); // newest first
            assertThat(result.getContent().get(1).getReferenceId()).isEqualTo(11L);
            assertThat(result.getContent().get(2).getReferenceId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("soft delete된 이벤트는 조회되지 않는다")
        void deletedEventsFiltered() {
            // given
            TimelineEvent active = saveEvent(1L, TimelineEventType.POST_CREATED, 10L, LocalDateTime.now());
            TimelineEvent deleted = saveEvent(1L, TimelineEventType.WALK_THREAD_CREATED, 11L, LocalDateTime.now().minusHours(1));
            deleted.markDeleted();
            timelineEventRepository.saveAndFlush(deleted);

            Pageable pageable = PageRequest.of(0, 20);

            // when
            Slice<TimelineEvent> result = timelineEventRepository
                    .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(1L, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getReferenceId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("다른 회원의 이벤트는 조회되지 않는다")
        void differentMemberNotIncluded() {
            // given
            saveEvent(1L, TimelineEventType.POST_CREATED, 10L, LocalDateTime.now());
            saveEvent(2L, TimelineEventType.POST_CREATED, 20L, LocalDateTime.now());

            Pageable pageable = PageRequest.of(0, 20);

            // when
            Slice<TimelineEvent> result = timelineEventRepository
                    .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(1L, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("빈 결과 시 빈 Slice를 반환한다")
        void emptyResult() {
            // given
            Pageable pageable = PageRequest.of(0, 20);

            // when
            Slice<TimelineEvent> result = timelineEventRepository
                    .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(999L, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("페이지네이션: 첫 페이지에 size만큼만 반환하고 hasNext를 설정한다")
        void pagination_firstPage() {
            // given
            LocalDateTime base = LocalDateTime.now();
            for (int i = 0; i < 5; i++) {
                saveEvent(1L, TimelineEventType.values()[i % TimelineEventType.values().length],
                        (long) (100 + i), base.minusMinutes(i));
            }

            Pageable pageable = PageRequest.of(0, 3);

            // when
            Slice<TimelineEvent> result = timelineEventRepository
                    .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(1L, pageable);

            // then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.isFirst()).isTrue();
        }

        @Test
        @DisplayName("페이지네이션: 마지막 페이지에서 hasNext가 false이다")
        void pagination_lastPage() {
            // given
            LocalDateTime base = LocalDateTime.now();
            for (int i = 0; i < 5; i++) {
                saveEvent(1L, TimelineEventType.values()[i % TimelineEventType.values().length],
                        (long) (200 + i), base.minusMinutes(i));
            }

            Pageable pageable = PageRequest.of(1, 3); // page 1, 2개 남음

            // when
            Slice<TimelineEvent> result = timelineEventRepository
                    .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(1L, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // findAllByEventTypeAndReferenceId
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("findAllByEventTypeAndReferenceId")
    class FindByEventTypeAndReferenceId {

        @Test
        @DisplayName("이벤트 타입과 참조 ID로 조회한다")
        void success() {
            // given
            saveEvent(1L, TimelineEventType.POST_CREATED, 42L, LocalDateTime.now());
            saveEvent(2L, TimelineEventType.WALK_THREAD_CREATED, 42L, LocalDateTime.now());

            // when
            List<TimelineEvent> result = timelineEventRepository
                    .findAllByEventTypeAndReferenceId(TimelineEventType.POST_CREATED, 42L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventType()).isEqualTo(TimelineEventType.POST_CREATED);
            assertThat(result.get(0).getReferenceId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("매칭되는 이벤트가 없으면 빈 리스트를 반환한다")
        void noMatch_empty() {
            // given
            saveEvent(1L, TimelineEventType.POST_CREATED, 42L, LocalDateTime.now());

            // when
            List<TimelineEvent> result = timelineEventRepository
                    .findAllByEventTypeAndReferenceId(TimelineEventType.WALK_THREAD_CREATED, 42L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("soft delete된 이벤트도 조회된다 (삭제 판단은 서비스 레이어)")
        void deletedEventsIncluded() {
            // given
            TimelineEvent event = saveEvent(1L, TimelineEventType.POST_CREATED, 42L, LocalDateTime.now());
            event.markDeleted();
            timelineEventRepository.saveAndFlush(event);

            // when
            List<TimelineEvent> result = timelineEventRepository
                    .findAllByEventTypeAndReferenceId(TimelineEventType.POST_CREATED, 42L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isDeleted()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // 영속성 검증
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("엔티티 영속성 검증")
    class Persistence {

        @Test
        @DisplayName("저장 후 조회하면 모든 필드가 유지된다")
        void saveAndRetrieve_allFieldsPreserved() {
            // given
            LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 8, 10, 30);
            TimelineEvent event = TimelineEvent.builder()
                    .memberId(1L)
                    .eventType(TimelineEventType.LOST_PET_REPORT_CREATED)
                    .referenceId(99L)
                    .title("실종된 강아지")
                    .summary("마포구에서 실종")
                    .thumbnailUrl("https://cdn.example.com/pet.jpg")
                    .occurredAt(occurredAt)
                    .build();

            // when
            TimelineEvent saved = timelineEventRepository.save(event);
            TimelineEvent found = timelineEventRepository.findById(saved.getId()).orElseThrow();

            // then
            assertThat(found.getMemberId()).isEqualTo(1L);
            assertThat(found.getEventType()).isEqualTo(TimelineEventType.LOST_PET_REPORT_CREATED);
            assertThat(found.getReferenceId()).isEqualTo(99L);
            assertThat(found.getTitle()).isEqualTo("실종된 강아지");
            assertThat(found.getSummary()).isEqualTo("마포구에서 실종");
            assertThat(found.getThumbnailUrl()).isEqualTo("https://cdn.example.com/pet.jpg");
            assertThat(found.getOccurredAt()).isEqualTo(occurredAt);
            assertThat(found.isDeleted()).isFalse();
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("markDeleted() 후 저장하면 deleted=true가 영속화된다")
        void markDeleted_persisted() {
            // given
            TimelineEvent event = saveEvent(1L, TimelineEventType.POST_CREATED, 50L, LocalDateTime.now());

            // when
            event.markDeleted();
            timelineEventRepository.saveAndFlush(event);
            TimelineEvent found = timelineEventRepository.findById(event.getId()).orElseThrow();

            // then
            assertThat(found.isDeleted()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private TimelineEvent saveEvent(Long memberId, TimelineEventType eventType, Long referenceId, LocalDateTime occurredAt) {
        return timelineEventRepository.save(TimelineEvent.builder()
                .memberId(memberId)
                .eventType(eventType)
                .referenceId(referenceId)
                .title("이벤트 " + referenceId)
                .occurredAt(occurredAt)
                .build());
    }
}
