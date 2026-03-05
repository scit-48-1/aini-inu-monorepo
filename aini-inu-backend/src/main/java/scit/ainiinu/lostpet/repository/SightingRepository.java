package scit.ainiinu.lostpet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.lostpet.domain.Sighting;

public interface SightingRepository extends JpaRepository<Sighting, Long> {
}
