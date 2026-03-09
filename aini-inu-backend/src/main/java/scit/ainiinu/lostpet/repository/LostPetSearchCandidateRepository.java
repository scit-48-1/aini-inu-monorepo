package scit.ainiinu.lostpet.repository;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;

public interface LostPetSearchCandidateRepository extends JpaRepository<LostPetSearchCandidate, Long> {

    @EntityGraph(attributePaths = "sighting")
    Slice<LostPetSearchCandidate> findBySessionIdOrderByRankOrderAsc(Long sessionId, Pageable pageable);

    Optional<LostPetSearchCandidate> findBySessionIdAndSightingId(Long sessionId, Long sightingId);
}
