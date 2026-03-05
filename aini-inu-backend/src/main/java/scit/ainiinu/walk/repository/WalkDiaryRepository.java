package scit.ainiinu.walk.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.walk.entity.WalkDiary;

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
}
