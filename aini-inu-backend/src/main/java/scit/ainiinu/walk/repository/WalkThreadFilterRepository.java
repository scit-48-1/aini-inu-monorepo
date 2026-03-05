package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.walk.entity.WalkThreadFilter;

public interface WalkThreadFilterRepository extends JpaRepository<WalkThreadFilter, Long> {

    void deleteAllByThreadId(Long threadId);
}
