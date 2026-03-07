package scit.ainiinu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.community.entity.PostLike;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    // 특정 게시글에 대해 특정 회원이 누른 좋아요 정보 조회
    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);

    // 좋아요 여부 확인
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    // 게시글의 모든 좋아요 삭제 (cascade 대용)
    void deleteAllByPostId(Long postId);
}
