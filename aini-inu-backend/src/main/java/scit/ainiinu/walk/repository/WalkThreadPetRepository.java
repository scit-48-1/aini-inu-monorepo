package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.walk.entity.WalkThreadPet;

import java.util.List;

public interface WalkThreadPetRepository extends JpaRepository<WalkThreadPet, Long> {

    List<WalkThreadPet> findAllByThreadId(Long threadId);

    void deleteAllByThreadId(Long threadId);
}
