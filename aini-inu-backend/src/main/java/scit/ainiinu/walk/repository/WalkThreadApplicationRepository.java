package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;

import java.util.List;
import java.util.Optional;

public interface WalkThreadApplicationRepository extends JpaRepository<WalkThreadApplication, Long> {

    Optional<WalkThreadApplication> findByThreadIdAndMemberId(Long threadId, Long memberId);

    Optional<WalkThreadApplication> findByThreadIdAndMemberIdAndStatus(
            Long threadId,
            Long memberId,
            WalkThreadApplicationStatus status
    );

    List<WalkThreadApplication> findAllByThreadIdAndStatus(Long threadId, WalkThreadApplicationStatus status);

    long countByThreadIdAndStatus(Long threadId, WalkThreadApplicationStatus status);
}
