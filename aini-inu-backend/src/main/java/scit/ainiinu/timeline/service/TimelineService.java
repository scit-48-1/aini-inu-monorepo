package scit.ainiinu.timeline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.timeline.dto.request.TimelineSettingsRequest;
import scit.ainiinu.timeline.dto.response.TimelineEventResponse;
import scit.ainiinu.timeline.dto.response.TimelineSettingsResponse;
import scit.ainiinu.timeline.entity.TimelineEvent;
import scit.ainiinu.timeline.exception.TimelineErrorCode;
import scit.ainiinu.timeline.repository.TimelineEventRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimelineService {

    private final TimelineEventRepository timelineEventRepository;
    private final MemberRepository memberRepository;

    public SliceResponse<TimelineEventResponse> getTimeline(Long requesterId, Long targetMemberId, Pageable pageable) {
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(TimelineErrorCode.MEMBER_NOT_FOUND));

        if (!requesterId.equals(targetMemberId) && !target.isTimelinePublic()) {
            throw new BusinessException(TimelineErrorCode.TIMELINE_NOT_PUBLIC);
        }

        Slice<TimelineEvent> slice = timelineEventRepository
                .findByMemberIdAndDeletedFalseOrderByOccurredAtDesc(targetMemberId, pageable);

        return SliceResponse.of(slice.map(TimelineEventResponse::from));
    }

    @Transactional
    public TimelineSettingsResponse updateSettings(Long memberId, TimelineSettingsRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(TimelineErrorCode.MEMBER_NOT_FOUND));

        member.updateTimelinePublic(request.getIsTimelinePublic());
        return new TimelineSettingsResponse(member.isTimelinePublic());
    }
}
