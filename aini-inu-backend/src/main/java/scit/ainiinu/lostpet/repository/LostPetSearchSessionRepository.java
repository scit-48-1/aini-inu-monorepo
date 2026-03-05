package scit.ainiinu.lostpet.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;

public interface LostPetSearchSessionRepository extends JpaRepository<LostPetSearchSession, Long> {

    Optional<LostPetSearchSession> findTopByOwnerIdAndLostPetReportIdOrderByCreatedAtDesc(Long ownerId, Long lostPetId);

    Optional<LostPetSearchSession> findByIdAndOwnerIdAndLostPetReportId(Long id, Long ownerId, Long lostPetId);
}
