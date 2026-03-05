package scit.ainiinu.pet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.pet.entity.Breed;

public interface BreedRepository extends JpaRepository<Breed, Long> {
}
