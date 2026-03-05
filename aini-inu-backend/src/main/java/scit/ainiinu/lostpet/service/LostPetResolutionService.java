package scit.ainiinu.lostpet.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetMatchInvalidatedReason;
import scit.ainiinu.lostpet.domain.LostPetMatchStatus;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.repository.LostPetMatchRepository;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;

@Service
@RequiredArgsConstructor
public class LostPetResolutionService {

    private final LostPetReportRepository lostPetReportRepository;
    private final LostPetMatchRepository lostPetMatchRepository;

    @Transactional
    public void resolveReport(Long memberId, Long lostPetId) {
        LostPetReport report = lostPetReportRepository.findById(lostPetId)
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        if (!report.getOwnerId().equals(memberId)) {
            throw new LostPetException(LostPetErrorCode.L403_FORBIDDEN);
        }
        report.resolve();

        List<LostPetMatch> activeMatches = lostPetMatchRepository.findByLostPetReportIdAndStatusIn(
                lostPetId,
                List.of(
                        LostPetMatchStatus.PENDING_APPROVAL,
                        LostPetMatchStatus.APPROVED,
                        LostPetMatchStatus.PENDING_CHAT_LINK
                )
        );
        for (LostPetMatch activeMatch : activeMatches) {
            activeMatch.invalidate(LostPetMatchInvalidatedReason.REPORT_RESOLVED);
        }
    }

    @Transactional(readOnly = true)
    public boolean isResolved(Long lostPetId) {
        LostPetReport report = lostPetReportRepository.findById(lostPetId)
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        return report.getStatus() == LostPetReportStatus.RESOLVED;
    }
}
