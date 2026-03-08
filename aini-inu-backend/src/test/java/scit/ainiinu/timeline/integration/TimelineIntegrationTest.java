package scit.ainiinu.timeline.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.ContentDeletedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.timeline.entity.TimelineEvent;
import scit.ainiinu.timeline.repository.TimelineEventRepository;
import scit.ainiinu.timeline.service.TimelineEventListener;
import scit.ainiinu.testsupport.IntegrationTestProfile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@IntegrationTestProfile
@Transactional
class TimelineIntegrationTest {

    @Autowired
    private TimelineEventListener timelineEventListener;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TimelineEventRepository timelineEventRepository;

    @Test
    @DisplayName("ContentCreatedEvent 발행 시 TimelineEvent가 저장된다")
    void contentCreated_savesTimelineEvent() {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("timeline-test@test.com")
                .nickname("타임라인유저")
                .memberType(MemberType.PET_OWNER)
                .build());

        ContentCreatedEvent event = ContentCreatedEvent.of(
                member.getId(), 42L, TimelineEventType.POST_CREATED,
                "테스트 게시글", "게시글 요약", null);

        // when
        timelineEventListener.onContentCreated(event);

        // then
        List<TimelineEvent> events = timelineEventRepository.findAllByEventTypeAndReferenceId(
                TimelineEventType.POST_CREATED, 42L);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getMemberId()).isEqualTo(member.getId());
        assertThat(events.get(0).getTitle()).isEqualTo("테스트 게시글");
        assertThat(events.get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("ContentDeletedEvent 발행 시 TimelineEvent가 soft delete된다")
    void contentDeleted_softDeletesTimelineEvent() {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("timeline-del@test.com")
                .nickname("삭제테스트")
                .memberType(MemberType.PET_OWNER)
                .build());

        // 먼저 생성
        ContentCreatedEvent created = ContentCreatedEvent.of(
                member.getId(), 100L, TimelineEventType.POST_CREATED,
                "삭제될 게시글", null, null);
        timelineEventListener.onContentCreated(created);

        // when - 삭제 이벤트 발행
        ContentDeletedEvent deleted = ContentDeletedEvent.of(
                member.getId(), 100L, TimelineEventType.POST_CREATED);
        timelineEventListener.onContentDeleted(deleted);

        // then
        List<TimelineEvent> events = timelineEventRepository.findAllByEventTypeAndReferenceId(
                TimelineEventType.POST_CREATED, 100L);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("비공개 타임라인 설정이 정상 동작한다")
    void privateTimeline_setting() {
        // given
        Member member = memberRepository.save(Member.builder()
                .email("private-tl@test.com")
                .nickname("비공개유저")
                .memberType(MemberType.PET_OWNER)
                .build());

        // when
        member.updateTimelinePublic(false);

        // then
        assertThat(member.isTimelinePublic()).isFalse();
    }
}
