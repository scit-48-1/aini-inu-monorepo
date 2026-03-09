package scit.ainiinu.lostpet.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetMatchCandidateResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostPetMatchQueryService {

    private final LostPetReportRepository lostPetReportRepository;
    private final LostPetSearchSessionRepository lostPetSearchSessionRepository;
    private final LostPetSearchCandidateRepository lostPetSearchCandidateRepository;
    private final MemberRepository memberRepository;

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

        Slice<LostPetSearchCandidate> candidateSlice =
                lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(session.getId(), pageable);

        List<Long> finderIds = candidateSlice.getContent().stream()
                .map(c -> c.getSighting().getFinderId())
                .distinct()
                .toList();
        Map<Long, String> nicknameMap = memberRepository.findAllById(finderIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));

        List<LostPetMatchCandidateResponse> responses = candidateSlice.getContent().stream()
                .map(candidate -> {
                    Sighting sighting = candidate.getSighting();
                    return LostPetMatchCandidateResponse.builder()
                            .sessionId(session.getId())
                            .sightingId(sighting.getId())
                            .finderId(sighting.getFinderId())
                            .photoUrl(sighting.getPhotoUrl())
                            .foundLocation(sighting.getFoundLocation())
                            .foundAt(sighting.getFoundAt())
                            .memo(sighting.getMemo())
                            .finderNickname(nicknameMap.getOrDefault(sighting.getFinderId(), "알 수 없음"))
                            .scoreSimilarity(candidate.getScoreSimilarity())
                            .scoreDistance(candidate.getScoreDistance())
                            .scoreRecency(candidate.getScoreRecency())
                            .scoreTotal(candidate.getScoreTotal())
                            .rank(candidate.getRankOrder())
                            .status(candidate.getStatus().name())
                            .build();
                })
                .toList();

        return new SliceImpl<>(responses, pageable, candidateSlice.hasNext());
    }
}
