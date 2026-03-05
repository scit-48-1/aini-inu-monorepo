package scit.ainiinu.pet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.pet.entity.Personality;

public interface PersonalityRepository extends JpaRepository<Personality, Long> {
}
