package scit.ainiinu.lostpet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.dto.LostPetCreateRequest;
import scit.ainiinu.lostpet.dto.LostPetDetailResponse;
import scit.ainiinu.lostpet.dto.LostPetResponse;
import scit.ainiinu.lostpet.dto.LostPetSummaryResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.repository.LostPetReportQueryRepository;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;

@Service
@RequiredArgsConstructor
public class LostPetServiceImpl implements LostPetService {

    private final LostPetReportRepository lostPetReportRepository;
    private final LostPetReportQueryRepository lostPetReportQueryRepository;

    @Override
    @Transactional
    public LostPetResponse create(Long memberId, LostPetCreateRequest request) {
        lostPetReportRepository.findActiveDuplicate(memberId, request.getPetName(), request.getBreed())
                .ifPresent(report -> {
                    throw new LostPetException(LostPetErrorCode.L409_DUPLICATE_ACTIVE_REPORT);
                });

        LostPetReport report = LostPetReport.create(
                memberId,
                request.getPetName(),
                request.getBreed(),
                request.getPhotoUrl(),
                request.getDescription(),
                request.getLastSeenAt(),
                request.getLastSeenLocation()
        );
        LostPetReport saved = lostPetReportRepository.save(report);

        return LostPetResponse.builder()
                .lostPetId(saved.getId())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<LostPetSummaryResponse> list(Long memberId, LostPetReportStatus status, Pageable pageable) {
        LostPetReportStatus targetStatus = status == null ? LostPetReportStatus.ACTIVE : status;
        return lostPetReportQueryRepository.findByOwnerAndStatus(memberId, targetStatus, pageable)
                .map(report -> LostPetSummaryResponse.builder()
                        .lostPetId(report.getId())
                        .petName(report.getPetName())
                        .status(report.getStatus().name())
                        .lastSeenAt(report.getLastSeenAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public LostPetDetailResponse detail(Long memberId, Long lostPetId) {
        LostPetReport report = lostPetReportRepository.findById(lostPetId)
                .orElseThrow(() -> new LostPetException(LostPetErrorCode.L404_NOT_FOUND));
        if (!report.getOwnerId().equals(memberId)) {
            throw new LostPetException(LostPetErrorCode.L403_FORBIDDEN);
        }
        return LostPetDetailResponse.builder()
                .lostPetId(report.getId())
                .ownerId(report.getOwnerId())
                .petName(report.getPetName())
                .photoUrl(report.getPhotoUrl())
                .lastSeenAt(report.getLastSeenAt())
                .lastSeenLocation(report.getLastSeenLocation())
                .status(report.getStatus().name())
                .build();
    }
}
