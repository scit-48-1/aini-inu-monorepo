package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.walk.entity.WalkThreadPet;

import java.util.List;

public interface WalkThreadPetRepository extends JpaRepository<WalkThreadPet, Long> {

    List<WalkThreadPet> findAllByThreadId(Long threadId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WalkThreadPet wtp WHERE wtp.threadId = :threadId")
    void deleteAllByThreadId(@Param("threadId") Long threadId);
}
