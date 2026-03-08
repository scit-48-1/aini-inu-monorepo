package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import scit.ainiinu.walk.entity.WalkThreadApplicationPet;

import java.util.List;

public interface WalkThreadApplicationPetRepository extends JpaRepository<WalkThreadApplicationPet, Long> {
    List<WalkThreadApplicationPet> findAllByApplicationId(Long applicationId);

    List<WalkThreadApplicationPet> findAllByApplicationIdIn(List<Long> applicationIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WalkThreadApplicationPet p WHERE p.applicationId = :applicationId")
    void deleteAllByApplicationId(Long applicationId);
}
