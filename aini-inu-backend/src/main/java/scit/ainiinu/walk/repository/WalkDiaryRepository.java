package scit.ainiinu.walk.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WalkDiaryRepository extends JpaRepository<WalkDiary, Long> {

    Slice<WalkDiary> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

    Slice<WalkDiary> findByMemberIdAndIsPublicTrueAndDeletedAtIsNull(Long memberId, Pageable pageable);

    Optional<WalkDiary> findByIdAndDeletedAtIsNull(Long diaryId);

    @Query("""
            select d
            from WalkDiary d
            join MemberFollow mf on mf.followingId = d.memberId
            where mf.followerId = :memberId
              and d.isPublic = true
              and d.deletedAt is null
            order by d.createdAt desc, d.id desc
            """)
    Slice<WalkDiary> findFollowingPublicSlice(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT d.threadId FROM WalkDiary d WHERE d.memberId = :memberId AND d.threadId IS NOT NULL AND d.deletedAt IS NULL")
    List<Long> findThreadIdsByMemberIdAndDeletedAtIsNull(@Param("memberId") Long memberId);

    boolean existsByMemberIdAndThreadIdAndDeletedAtIsNull(Long memberId, Long threadId);

    @Query("""
            select d.walkDate as walkDate, count(d) as walkCount
            from WalkDiary d
            where d.memberId = :memberId
              and d.deletedAt is null
              and d.walkDate between :startDate and :endDate
            group by d.walkDate
            """)
    List<WalkDiaryDailyCountProjection> countDailyWalks(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
