package scit.ainiinu.walk.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalkThreadRepository extends JpaRepository<WalkThread, Long> {

    boolean existsByAuthorIdAndStatus(Long authorId, WalkThreadStatus status);

    Optional<WalkThread> findByIdAndStatusNot(Long threadId, WalkThreadStatus excludedStatus);

    Slice<WalkThread> findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus status, Pageable pageable);

    List<WalkThread> findByStatus(WalkThreadStatus status);

    List<WalkThread> findAllByAuthorIdAndStatus(Long authorId, WalkThreadStatus status);

    @Query("select t from WalkThread t where t.status = :status and t.createdAt >= :createdAfter")
    List<WalkThread> findByStatusAndCreatedAfter(
            @Param("status") WalkThreadStatus status,
            @Param("createdAfter") LocalDateTime createdAfter
    );
}
