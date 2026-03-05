package scit.ainiinu.pet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.pet.entity.WalkingStyle;

public interface WalkingStyleRepository extends JpaRepository<WalkingStyle, Long> {
    java.util.List<WalkingStyle> findByCodeIn(java.util.Collection<String> codes);
}
