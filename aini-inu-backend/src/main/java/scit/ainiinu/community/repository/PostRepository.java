package scit.ainiinu.community.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.community.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 페이지 조회
    Slice<Post> findAllBy(Pageable pageable);

    //최신 글
    List<Post> findAllByOrderByCreatedAtDesc();
    //특정 아이디의 게시글
    List<Post> findAllByAuthorId(Long authorId);
    //좋아요 많은 순
    List<Post> findAllByOrderByLikeCountDesc();
    //댓글 많은 순
    List<Post> findAllByOrderByCommentCountDesc();

}
