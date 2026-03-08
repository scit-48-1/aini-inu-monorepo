package scit.ainiinu.timeline.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.timeline.dto.request.TimelineSettingsRequest;
import scit.ainiinu.timeline.dto.response.TimelineEventResponse;
import scit.ainiinu.timeline.dto.response.TimelineSettingsResponse;
import scit.ainiinu.timeline.entity.TimelineEvent;
import scit.ainiinu.timeline.exception.TimelineErrorCode;
import scit.ainiinu.timeline.repository.TimelineEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    @Mock
    private TimelineEventRepository timelineEventRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TimelineService timelineService;

    // ---------------------------------------------------------------
    // getTimeline
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("타임라인 조회 (getTimeline)")
    class GetTimeline {

        @Test
        @DisplayName("성공: 본인 타임라인 조회")
        void getTimeline_self_success() {
            // given
            Long memberId = 1L;
            Member member = createMember(memberId, true);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            Pageable pageable = PageRequest.of(0, 20);
            TimelineEvent event = createTimelineEvent(1L, memberId, TimelineEventType.POST_CREATED);
            given(timelineEventRepository.findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(eq(memberId), any()))
                    .willReturn(new SliceImpl<>(List.of(event), pageable, false));

            // when
            SliceResponse<TimelineEventResponse> result = timelineService.getTimeline(memberId, memberId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEventType()).isEqualTo(TimelineEventType.POST_CREATED);
        }

        @Test
        @DisplayName("성공: 타인의 공개 타임라인 조회")
        void getTimeline_otherPublic_success() {
            // given
            Long requesterId = 1L;
            Long targetId = 2L;
            Member target = createMember(targetId, true);
            given(memberRepository.findById(targetId)).willReturn(Optional.of(target));

            Pageable pageable = PageRequest.of(0, 20);
            given(timelineEventRepository.findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(eq(targetId), any()))
                    .willReturn(new SliceImpl<>(List.of(), pageable, false));

            // when
            SliceResponse<TimelineEventResponse> result = timelineService.getTimeline(requesterId, targetId, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("실패: 비공개 타임라인을 타인이 조회하면 403")
        void getTimeline_otherPrivate_forbidden() {
            // given
            Long requesterId = 1L;
            Long targetId = 2L;
            Member target = createMember(targetId, false);
            given(memberRepository.findById(targetId)).willReturn(Optional.of(target));

            Pageable pageable = PageRequest.of(0, 20);

            // when & then
            assertThatThrownBy(() -> timelineService.getTimeline(requesterId, targetId, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TimelineErrorCode.TIMELINE_NOT_PUBLIC);
        }

        @Test
        @DisplayName("성공: 비공개 타임라인이라도 본인은 조회 가능")
        void getTimeline_selfPrivate_success() {
            // given
            Long memberId = 1L;
            Member member = createMember(memberId, false);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            Pageable pageable = PageRequest.of(0, 20);
            given(timelineEventRepository.findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(eq(memberId), any()))
                    .willReturn(new SliceImpl<>(List.of(), pageable, false));

            // when
            SliceResponse<TimelineEventResponse> result = timelineService.getTimeline(memberId, memberId, pageable);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 조회 시 404")
        void getTimeline_memberNotFound() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> timelineService.getTimeline(1L, 999L, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TimelineErrorCode.MEMBER_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------
    // updateSettings
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("타임라인 설정 변경 (updateSettings)")
    class UpdateSettings {

        @Test
        @DisplayName("성공: 공개→비공개 설정 변경")
        void updateSettings_toPrivate() {
            // given
            Long memberId = 1L;
            Member member = createMember(memberId, true);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            TimelineSettingsRequest request = new TimelineSettingsRequest();
            request.setIsTimelinePublic(false);

            // when
            TimelineSettingsResponse response = timelineService.updateSettings(memberId, request);

            // then
            assertThat(response.isTimelinePublic()).isFalse();
            assertThat(member.isTimelinePublic()).isFalse();
        }

        @Test
        @DisplayName("성공: 비공개→공개 설정 변경")
        void updateSettings_toPublic() {
            // given
            Long memberId = 1L;
            Member member = createMember(memberId, false);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            TimelineSettingsRequest request = new TimelineSettingsRequest();
            request.setIsTimelinePublic(true);

            // when
            TimelineSettingsResponse response = timelineService.updateSettings(memberId, request);

            // then
            assertThat(response.isTimelinePublic()).isTrue();
            assertThat(member.isTimelinePublic()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원의 설정 변경 시 404")
        void updateSettings_memberNotFound() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            TimelineSettingsRequest request = new TimelineSettingsRequest();
            request.setIsTimelinePublic(false);

            // when & then
            assertThatThrownBy(() -> timelineService.updateSettings(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", TimelineErrorCode.MEMBER_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Member createMember(Long id, boolean isTimelinePublic) {
        Member member = Member.builder()
                .email("user" + id + "@test.com")
                .nickname("유저" + id)
                .memberType(MemberType.PET_OWNER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        member.updateTimelinePublic(isTimelinePublic);
        return member;
    }

    private TimelineEvent createTimelineEvent(Long id, Long memberId, TimelineEventType eventType) {
        TimelineEvent event = TimelineEvent.builder()
                .memberId(memberId)
                .eventType(eventType)
                .referenceId(42L)
                .title("테스트 이벤트")
                .summary("요약")
                .occurredAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
