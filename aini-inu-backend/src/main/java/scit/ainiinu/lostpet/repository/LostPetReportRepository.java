package scit.ainiinu.lostpet.repository;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;

public interface LostPetReportRepository extends JpaRepository<LostPetReport, Long> {

    @Query("""
            select l
            from LostPetReport l
            where l.ownerId = :ownerId
              and l.petName = :petName
              and (:breed is null or l.breed = :breed)
              and l.status = scit.ainiinu.lostpet.domain.LostPetReportStatus.ACTIVE
            """)
    Optional<LostPetReport> findActiveDuplicate(
            @Param("ownerId") Long ownerId,
            @Param("petName") String petName,
            @Param("breed") String breed
    );

    Slice<LostPetReport> findByOwnerIdAndStatus(Long ownerId, LostPetReportStatus status, Pageable pageable);
}
