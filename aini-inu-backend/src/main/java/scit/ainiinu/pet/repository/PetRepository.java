package scit.ainiinu.pet.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.pet.entity.Pet;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Integer countByMemberId(Long memberId);
    java.util.Optional<Pet> findByMemberIdAndIsMainTrue(Long memberId);
    List<Pet> findAllByMemberIdOrderByIsMainDesc(Long memberId);
}
