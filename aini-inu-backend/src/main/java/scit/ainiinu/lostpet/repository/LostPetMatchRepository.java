package scit.ainiinu.lostpet.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetMatchStatus;

public interface LostPetMatchRepository extends JpaRepository<LostPetMatch, Long> {

    List<LostPetMatch> findByLostPetReportId(Long lostPetReportId);

    Slice<LostPetMatch> findByLostPetReportId(Long lostPetReportId, Pageable pageable);

    Optional<LostPetMatch> findByLostPetReportIdAndSightingId(Long lostPetReportId, Long sightingId);

    List<LostPetMatch> findByLostPetReportIdAndStatusIn(Long lostPetReportId, Collection<LostPetMatchStatus> statuses);
}
