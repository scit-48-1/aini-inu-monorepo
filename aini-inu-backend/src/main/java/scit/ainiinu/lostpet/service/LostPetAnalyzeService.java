package scit.ainiinu.lostpet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeCandidateResponse;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.integration.ai.LostPetAiCandidate;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClient;
import scit.ainiinu.lostpet.integration.ai.LostPetAiResult;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.repository.SightingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostPetAnalyzeService {

    private final LostPetAiClient lostPetAiClient;
    private final LostPetReportRepository lostPetReportRepository;
    private final SightingRepository sightingRepository;
    private final LostPetSearchSessionRepository lostPetSearchSessionRepository;
    private final LostPetSearchCandidateRepository lostPetSearchCandidateRepository;
    private final LostPetCandidateScoringService lostPetCandidateScoringService;

    @Value("${lostpet.search.session-ttl-hours:24}")
    private long sessionTtlHours;

    @Value("${lostpet.search.top-n:20}")
    private int topN;

    @Transactional
    public LostPetAnalyzeResponse analyze(Long memberId, LostPetAnalyzeRequest request) {
        long startedAt = System.currentTimeMillis();
        LostPetReport report = lostPetReportRepository.findById(request.getLostPetId())
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        if (!report.getOwnerId().equals(memberId)) {
            throw new LostPetException(LostPetErrorCode.L403_FORBIDDEN);
        }

        enrichQueryTextFromReport(request, report);
        LostPetAiResult result = analyzeWithAiOrThrow(request, report, startedAt);
        List<LostPetAiCandidate> aiCandidates = result.candidates() == null
                ? List.of()
                : result.candidates();

        LostPetSearchSession session = lostPetSearchSessionRepository.save(
                LostPetSearchSession.create(
                        memberId,
                        report,
                        request.resolveMode(),
                        request.resolveImageSource(),
                        request.getQueryText(),
                        LocalDateTime.now().plusHours(sessionTtlHours)
                )
        );

        List<ScoredCandidate> scoredCandidates = aiCandidates.stream()
                .map(aiCandidate -> toScoredCandidate(report, aiCandidate))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(ScoredCandidate::getScoreTotal).reversed()
                        .thenComparing(scoredCandidate -> scoredCandidate.getSighting().getId()))
                .limit(topN)
                .toList();

        int rankOrder = 1;
        List<LostPetSearchCandidate> entities = new java.util.ArrayList<>(scoredCandidates.size());
        for (ScoredCandidate scoredCandidate : scoredCandidates) {
            entities.add(LostPetSearchCandidate.create(
                    session,
                    scoredCandidate.getSighting(),
                    scoredCandidate.getScoreSimilarity(),
                    scoredCandidate.getScoreDistance(),
                    scoredCandidate.getScoreRecency(),
                    scoredCandidate.getScoreTotal(),
                    rankOrder++
            ));
        }

        List<LostPetSearchCandidate> savedCandidates = entities.isEmpty()
                ? List.of()
                : lostPetSearchCandidateRepository.saveAll(entities);

        List<LostPetAnalyzeCandidateResponse> candidates = savedCandidates.stream()
                .map(this::toCandidateResponse)
                .toList();
        log.info(
                "lostpet.analyze success mode={} lostPetId={} sessionId={} candidateCount={} elapsedMs={}",
                request.resolveMode(),
                report.getId(),
                session.getId(),
                candidates.size(),
                System.currentTimeMillis() - startedAt
        );
        return LostPetAnalyzeResponse.builder()
                .sessionId(session.getId())
                .summary(result.summary() == null ? "" : result.summary())
                .candidates(candidates)
                .build();
    }

    private void enrichQueryTextFromReport(LostPetAnalyzeRequest request, LostPetReport report) {
        StringBuilder enriched = new StringBuilder();
        if (report.getPetName() != null && !report.getPetName().isBlank()) {
            enriched.append(report.getPetName()).append(' ');
        }
        if (report.getBreed() != null && !report.getBreed().isBlank()) {
            enriched.append(report.getBreed()).append(' ');
        }
        if (report.getDescription() != null && !report.getDescription().isBlank()) {
            enriched.append(report.getDescription()).append(' ');
        }
        if (report.getLastSeenLocation() != null && !report.getLastSeenLocation().isBlank()) {
            enriched.append(report.getLastSeenLocation()).append(' ');
        }
        if (request.getQueryText() != null && !request.getQueryText().isBlank()) {
            enriched.append(request.getQueryText());
        }
        String result = enriched.toString().trim();
        if (!result.isEmpty()) {
            request.setQueryText(result);
        }
    }

    private LostPetAiResult analyzeWithAiOrThrow(
            LostPetAnalyzeRequest request,
            LostPetReport report,
            long startedAt
    ) {
        try {
            LostPetAiResult result = lostPetAiClient.analyze(request.normalizeForAi());
            if (result == null) {
                log.warn(
                        "lostpet.analyze failed mode={} lostPetId={} elapsedMs={} reason=result-null",
                        request.resolveMode(),
                        report.getId(),
                        System.currentTimeMillis() - startedAt
                );
                throw new LostPetException(LostPetErrorCode.L500_AI_ANALYZE_FAILED);
            }
            return result;
        } catch (LostPetException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "lostpet.analyze failed mode={} lostPetId={} elapsedMs={} reason={}",
                    request.resolveMode(),
                    report.getId(),
                    System.currentTimeMillis() - startedAt,
                    exception.getClass().getSimpleName()
            );
            throw new LostPetException(LostPetErrorCode.L500_AI_ANALYZE_FAILED);
        }
    }

    private List<ScoredCandidate> toScoredCandidate(LostPetReport report, LostPetAiCandidate aiCandidate) {
        if (aiCandidate.sightingId() == null) {
            return List.of();
        }
        return sightingRepository.findById(aiCandidate.sightingId())
                .map(sighting -> {
                    BigDecimal scoreSimilarity = lostPetCandidateScoringService.normalizeSimilarity(
                            aiCandidate.similarityTotal()
                    );
                    BigDecimal scoreDistance = lostPetCandidateScoringService.computeDistanceScore(
                            report.getLastSeenLocation(),
                            sighting.getFoundLocation()
                    );
                    BigDecimal scoreRecency = lostPetCandidateScoringService.computeRecencyScore(
                            report.getLastSeenAt(),
                            sighting.getFoundAt()
                    );
                    BigDecimal scoreTotal = lostPetCandidateScoringService.computeTotalScore(
                            scoreSimilarity,
                            scoreDistance,
                            scoreRecency
                    );
                    return List.of(new ScoredCandidate(sighting, scoreSimilarity, scoreDistance, scoreRecency, scoreTotal));
                })
                .orElseGet(List::of);
    }

    private LostPetAnalyzeCandidateResponse toCandidateResponse(LostPetSearchCandidate candidate) {
        return LostPetAnalyzeCandidateResponse.builder()
                .sightingId(candidate.getSighting().getId())
                .finderId(candidate.getSighting().getFinderId())
                .scoreSimilarity(candidate.getScoreSimilarity())
                .scoreDistance(candidate.getScoreDistance())
                .scoreRecency(candidate.getScoreRecency())
                .scoreTotal(candidate.getScoreTotal())
                .rank(candidate.getRankOrder())
                .status(candidate.getStatus().name())
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    private static class ScoredCandidate {
        private final Sighting sighting;
        private final BigDecimal scoreSimilarity;
        private final BigDecimal scoreDistance;
        private final BigDecimal scoreRecency;
        private final BigDecimal scoreTotal;
    }
}
