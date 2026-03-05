package scit.ainiinu.lostpet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.dto.LostPetMatchCandidateResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostPetMatchQueryService {

    private final LostPetReportRepository lostPetReportRepository;
    private final LostPetSearchSessionRepository lostPetSearchSessionRepository;
    private final LostPetSearchCandidateRepository lostPetSearchCandidateRepository;

    public Slice<LostPetMatchCandidateResponse> findCandidates(
            Long lostPetId,
            Long memberId,
            Long sessionId,
            Pageable pageable
    ) {
        LostPetReport report = lostPetReportRepository.findById(lostPetId)
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        if (!report.getOwnerId().equals(memberId)) {
            throw new LostPetException(LostPetErrorCode.L403_FORBIDDEN);
        }

        LostPetSearchSession session = sessionId == null
                ? lostPetSearchSessionRepository.findTopByOwnerIdAndLostPetReportIdOrderByCreatedAtDesc(memberId, lostPetId)
                        .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_SEARCH_SESSION_NOT_FOUND))
                : lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(sessionId, memberId, lostPetId)
                        .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_SEARCH_SESSION_NOT_FOUND));

        if (session.isExpired(LocalDateTime.now())) {
            throw new LostPetException(LostPetErrorCode.L410_SEARCH_SESSION_EXPIRED);
        }

        return lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(session.getId(), pageable)
                .map(candidate -> LostPetMatchCandidateResponse.builder()
                        .sessionId(session.getId())
                        .sightingId(candidate.getSighting().getId())
                        .finderId(candidate.getSighting().getFinderId())
                        .scoreSimilarity(candidate.getScoreSimilarity())
                        .scoreDistance(candidate.getScoreDistance())
                        .scoreRecency(candidate.getScoreRecency())
                        .scoreTotal(candidate.getScoreTotal())
                        .rank(candidate.getRankOrder())
                        .status(candidate.getStatus().name())
                        .build());
    }
}
