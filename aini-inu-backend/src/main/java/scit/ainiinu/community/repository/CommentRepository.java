package scit.ainiinu.community.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.community.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 게시글에 달린 댓글 목록 조회 (작성일 오름차순)
    List<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId);

    // 게시글 댓글 목록 슬라이스 조회 (무한 스크롤)
    Slice<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    // 게시글의 모든 댓글 삭제 (cascade 대용)
    void deleteAllByPostId(Long postId);
}
