package scit.ainiinu.timeline.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.ContentDeletedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.timeline.entity.TimelineEvent;
import scit.ainiinu.timeline.repository.TimelineEventRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TimelineEventListenerTest {

    @Mock
    private TimelineEventRepository timelineEventRepository;

    @InjectMocks
    private TimelineEventListener timelineEventListener;

    @Nested
    @DisplayName("콘텐츠 생성 이벤트 (onContentCreated)")
    class OnContentCreated {

        @Test
        @DisplayName("성공: ContentCreatedEvent 수신 시 TimelineEvent가 저장된다")
        void onContentCreated_saves() {
            // given
            ContentCreatedEvent event = ContentCreatedEvent.of(
                    1L, 42L, TimelineEventType.POST_CREATED,
                    "제목", "요약", "https://img.com/thumb.jpg");

            given(timelineEventRepository.save(any(TimelineEvent.class))).willAnswer(invocation -> {
                TimelineEvent saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 100L);
                return saved;
            });

            // when
            timelineEventListener.onContentCreated(event);

            // then
            ArgumentCaptor<TimelineEvent> captor = ArgumentCaptor.forClass(TimelineEvent.class);
            then(timelineEventRepository).should().save(captor.capture());

            TimelineEvent saved = captor.getValue();
            assertThat(saved.getMemberId()).isEqualTo(1L);
            assertThat(saved.getReferenceId()).isEqualTo(42L);
            assertThat(saved.getEventType()).isEqualTo(TimelineEventType.POST_CREATED);
            assertThat(saved.getTitle()).isEqualTo("제목");
            assertThat(saved.getSummary()).isEqualTo("요약");
            assertThat(saved.getThumbnailUrl()).isEqualTo("https://img.com/thumb.jpg");
            assertThat(saved.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("성공: nullable 필드가 null이어도 저장된다")
        void onContentCreated_nullFields() {
            // given
            ContentCreatedEvent event = ContentCreatedEvent.of(
                    1L, 42L, TimelineEventType.WALKING_SESSION_STARTED,
                    "산책 시작", null, null);

            given(timelineEventRepository.save(any())).willAnswer(i -> i.getArgument(0));

            // when
            timelineEventListener.onContentCreated(event);

            // then
            ArgumentCaptor<TimelineEvent> captor = ArgumentCaptor.forClass(TimelineEvent.class);
            then(timelineEventRepository).should().save(captor.capture());
            assertThat(captor.getValue().getSummary()).isNull();
            assertThat(captor.getValue().getThumbnailUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("콘텐츠 삭제 이벤트 (onContentDeleted)")
    class OnContentDeleted {

        @Test
        @DisplayName("성공: ContentDeletedEvent 수신 시 해당 TimelineEvent가 soft delete된다")
        void onContentDeleted_softDeletes() {
            // given
            ContentDeletedEvent event = ContentDeletedEvent.of(1L, 42L, TimelineEventType.POST_CREATED);

            TimelineEvent existing = TimelineEvent.builder()
                    .memberId(1L)
                    .eventType(TimelineEventType.POST_CREATED)
                    .referenceId(42L)
                    .title("게시글")
                    .occurredAt(java.time.LocalDateTime.now())
                    .build();
            ReflectionTestUtils.setField(existing, "id", 100L);

            given(timelineEventRepository.findAllByEventTypeAndReferenceId(
                    TimelineEventType.POST_CREATED, 42L))
                    .willReturn(List.of(existing));

            // when
            timelineEventListener.onContentDeleted(event);

            // then
            assertThat(existing.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("성공: 해당 이벤트가 없으면 아무것도 하지 않는다")
        void onContentDeleted_noMatch() {
            // given
            ContentDeletedEvent event = ContentDeletedEvent.of(1L, 999L, TimelineEventType.POST_CREATED);

            given(timelineEventRepository.findAllByEventTypeAndReferenceId(
                    TimelineEventType.POST_CREATED, 999L))
                    .willReturn(List.of());

            // when
            timelineEventListener.onContentDeleted(event);

            // then
            then(timelineEventRepository).should().findAllByEventTypeAndReferenceId(
                    TimelineEventType.POST_CREATED, 999L);
        }
    }
}
