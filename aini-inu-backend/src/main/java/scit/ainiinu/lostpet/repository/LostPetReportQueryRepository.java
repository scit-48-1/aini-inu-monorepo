package scit.ainiinu.lostpet.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;

@Repository
@RequiredArgsConstructor
public class LostPetReportQueryRepository {

    private final LostPetReportRepository lostPetReportRepository;

    public Slice<LostPetReport> findByOwnerAndStatus(Long ownerId, LostPetReportStatus status, Pageable pageable) {
        return lostPetReportRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
    }
}
