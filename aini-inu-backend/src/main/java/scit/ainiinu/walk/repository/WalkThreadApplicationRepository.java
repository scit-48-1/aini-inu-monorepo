package scit.ainiinu.walk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT a.threadId, COUNT(a) FROM WalkThreadApplication a " +
            "WHERE a.threadId IN :threadIds AND a.status = :status GROUP BY a.threadId")
    List<Object[]> countByThreadIdInAndStatus(
            @Param("threadIds") List<Long> threadIds,
            @Param("status") WalkThreadApplicationStatus status
    );

    @Query("SELECT a FROM WalkThreadApplication a " +
            "WHERE a.threadId IN :threadIds AND a.memberId = :memberId AND a.status = :status")
    List<WalkThreadApplication> findByThreadIdInAndMemberIdAndStatus(
            @Param("threadIds") List<Long> threadIds,
            @Param("memberId") Long memberId,
            @Param("status") WalkThreadApplicationStatus status
    );
}
