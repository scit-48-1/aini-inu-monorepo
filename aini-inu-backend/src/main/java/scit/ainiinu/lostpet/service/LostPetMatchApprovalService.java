package scit.ainiinu.lostpet.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetMatchStatus;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.domain.SightingStatus;
import scit.ainiinu.lostpet.dto.LostPetMatchApproveRequest;
import scit.ainiinu.lostpet.dto.LostPetMatchResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.integration.chat.ChatDirectClientException;
import scit.ainiinu.lostpet.integration.chat.ChatDirectFailureType;
import scit.ainiinu.lostpet.integration.chat.ChatRoomDirectClient;
import scit.ainiinu.lostpet.repository.LostPetMatchRepository;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.repository.SightingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostPetMatchApprovalService {

    private final LostPetReportRepository lostPetReportRepository;
    private final SightingRepository sightingRepository;
    private final LostPetMatchRepository lostPetMatchRepository;
    private final LostPetSearchSessionRepository lostPetSearchSessionRepository;
    private final LostPetSearchCandidateRepository lostPetSearchCandidateRepository;
    private final ChatRoomDirectClient chatRoomDirectClient;

    @Transactional
    public LostPetMatchResponse approve(
            Long lostPetId,
            Long memberId,
            LostPetMatchApproveRequest request,
            String authorizationHeader
    ) {
        long startedAt = System.currentTimeMillis();
        LostPetReport report = lostPetReportRepository.findById(lostPetId)
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        if (!report.getOwnerId().equals(memberId)) {
            throw new LostPetException(LostPetErrorCode.L403_FORBIDDEN);
        }
        if (report.getStatus() != LostPetReportStatus.ACTIVE) {
            throw new LostPetException(LostPetErrorCode.L410_REPORT_RESOLVED);
        }

        LostPetSearchSession session = lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(
                        request.getSessionId(),
                        memberId,
                        lostPetId
                )
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_SEARCH_SESSION_NOT_FOUND));
        if (session.isExpired(LocalDateTime.now())) {
            throw new LostPetException(LostPetErrorCode.L410_SEARCH_SESSION_EXPIRED);
        }

        Long sightingId = request.getSightingId();
        LostPetSearchCandidate candidate = lostPetSearchCandidateRepository.findBySessionIdAndSightingId(
                        session.getId(),
                        sightingId
                )
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L409_SEARCH_CANDIDATE_INVALID));

        Sighting sighting = sightingRepository.findById(sightingId)
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        if (sighting.getStatus() == SightingStatus.CLOSED) {
            throw new LostPetException(LostPetErrorCode.L409_MATCH_CONFLICT);
        }

        LostPetMatch match = lostPetMatchRepository.findByLostPetReportIdAndSightingId(lostPetId, sightingId)
                .orElseGet(() -> LostPetMatch.create(report, sighting, candidate.getScoreTotal()));

        LostPetMatchStatus status = match.getStatus();
        if (status == LostPetMatchStatus.INVALIDATED || status == LostPetMatchStatus.REJECTED) {
            throw new LostPetException(LostPetErrorCode.L409_MATCH_CONFLICT);
        }
        if (status == LostPetMatchStatus.CHAT_LINKED) {
            candidate.approve();
            return toResponse(match);
        }
        if (status == LostPetMatchStatus.PENDING_APPROVAL) {
            match.approve(memberId);
        }
        candidate.approve();

        try {
            String roomTitle = (report.getBreed() != null ? report.getBreed() + " " : "") + report.getPetName() + "를 찾습니다";
            Long chatRoomId = chatRoomDirectClient.createDirectRoom(sighting.getFinderId(), "LOST_PET", roomTitle, authorizationHeader);
            if (chatRoomId == null) {
                throw new ChatDirectClientException(
                        ChatDirectFailureType.RESPONSE_SCHEMA,
                        "chatRoomId is null"
                );
            }
            match.linkChatRoom(chatRoomId);
            log.info(
                    "lostpet.match.approve chat-linked lostPetId={} sightingId={} sessionId={} memberId={} chatRoomId={} elapsedMs={}",
                    lostPetId,
                    sightingId,
                    session.getId(),
                    memberId,
                    chatRoomId,
                    System.currentTimeMillis() - startedAt
            );
        } catch (ChatDirectClientException exception) {
            match.markPendingChatLink();
            log.warn(
                    "lostpet.match.approve chat-create-failed lostPetId={} sightingId={} sessionId={} memberId={} elapsedMs={} reason={} failureType={}",
                    lostPetId,
                    sightingId,
                    session.getId(),
                    memberId,
                    System.currentTimeMillis() - startedAt,
                    exception.getClass().getSimpleName(),
                    exception.getFailureType()
            );
            if (exception.getFailureType() == ChatDirectFailureType.AUTH) {
                log.warn("lostpet.match.approve chat-auth-failed lostPetId={} sessionId={}", lostPetId, session.getId());
            } else if (exception.getFailureType() == ChatDirectFailureType.CONNECT) {
                log.warn("lostpet.match.approve chat-connect-failed lostPetId={} sessionId={}", lostPetId, session.getId());
            } else if (exception.getFailureType() == ChatDirectFailureType.RESPONSE_SCHEMA) {
                log.warn("lostpet.match.approve chat-schema-failed lostPetId={} sessionId={}", lostPetId, session.getId());
            }
        }

        LostPetMatch saved = lostPetMatchRepository.save(match);
        return toResponse(saved);
    }

    private LostPetMatchResponse toResponse(LostPetMatch match) {
        return LostPetMatchResponse.builder()
                .matchId(match.getId())
                .status(match.getStatus().name())
                .chatRoomId(match.getChatRoomId())
                .build();
    }
}
