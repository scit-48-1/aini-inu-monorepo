package scit.ainiinu.member.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.member.entity.MemberFollow;

import java.util.List;
import java.util.Optional;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, Long> {
    Slice<MemberFollow> findAllByFollowingIdOrderByCreatedAtDesc(Long followingId, Pageable pageable);

    Slice<MemberFollow> findAllByFollowerIdOrderByCreatedAtDesc(Long followerId, Pageable pageable);

    Optional<MemberFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Query("select mf.followingId from MemberFollow mf where mf.followerId = :followerId")
    List<Long> findFollowingIdsByFollowerId(@Param("followerId") Long followerId);
}
