package scit.ainiinu.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.dto.response.WalkingSessionResponse;
import scit.ainiinu.walk.dto.response.WalkingUserResponse;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;
import scit.ainiinu.walk.exception.WalkingSessionErrorCode;
import scit.ainiinu.walk.repository.WalkingSessionRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkingSessionService {

    private final WalkingSessionRepository walkingSessionRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WalkingSessionResponse startSession(Long memberId) {
        walkingSessionRepository.findByMemberIdAndStatus(memberId, WalkingSessionStatus.ACTIVE)
                .ifPresent(s -> {
                    throw new BusinessException(WalkingSessionErrorCode.WALKING_SESSION_ALREADY_ACTIVE);
                });

        WalkingSession session = WalkingSession.create(memberId);
        walkingSessionRepository.save(session);

        eventPublisher.publishEvent(ContentCreatedEvent.of(
                memberId, session.getId(), TimelineEventType.WALKING_SESSION_STARTED,
                "산책 시작", null, null));

        return WalkingSessionResponse.from(session);
    }

    @Transactional
    public void heartbeat(Long memberId) {
        WalkingSession session = walkingSessionRepository.findByMemberIdAndStatus(memberId, WalkingSessionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND));
        session.refreshHeartbeat();
    }

    @Transactional
    public void stopSession(Long memberId) {
        WalkingSession session = walkingSessionRepository.findByMemberIdAndStatus(memberId, WalkingSessionStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND));
        session.end();

        eventPublisher.publishEvent(ContentCreatedEvent.of(
                memberId, session.getId(), TimelineEventType.WALKING_SESSION_COMPLETED,
                "산책 완료", null, null));
    }

    public List<WalkingUserResponse> getActiveWalkers() {
        List<WalkingSession> activeSessions = walkingSessionRepository.findAllByStatus(WalkingSessionStatus.ACTIVE);
        if (activeSessions.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = activeSessions.stream()
                .map(WalkingSession::getMemberId)
                .toList();

        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return activeSessions.stream()
                .filter(s -> memberMap.containsKey(s.getMemberId()))
                .map(s -> {
                    Member member = memberMap.get(s.getMemberId());
                    return new WalkingUserResponse(
                            member.getId(),
                            member.getNickname(),
                            member.getProfileImageUrl(),
                            member.getMannerTemperature().getValue(),
                            s.getStartedAt()
                    );
                })
                .toList();
    }

    public WalkingSessionResponse getMySession(Long memberId) {
        return walkingSessionRepository.findByMemberIdAndStatus(memberId, WalkingSessionStatus.ACTIVE)
                .map(WalkingSessionResponse::from)
                .orElse(null);
    }
}
