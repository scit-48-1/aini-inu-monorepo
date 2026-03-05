package scit.ainiinu.community.dto;

import lombok.Data;
import scit.ainiinu.community.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDetailResponse {
    private Long id;
    private PostResponse.Author author;
    private String content;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
    private List<CommentResponse> comments;

    /**
     * 좋아요 여부를 포함하여 PostDetailResponse 생성
     * @param post 게시글 엔티티
     * @param author 작성자 정보
     * @param comments 댓글 목록
     * @param isLiked 현재 사용자의 좋아요 여부
     * @return PostDetailResponse
     */
    public static PostDetailResponse of(
            Post post,
            PostResponse.Author author,
            List<CommentResponse> comments,
            boolean isLiked
    ) {
        PostDetailResponse r = new PostDetailResponse();
        r.id = post.getId();
        r.content = post.getContent();
        r.imageUrls = post.getImageUrls();
        r.likeCount = post.getLikeCount();
        r.commentCount = post.getCommentCount();
        r.isLiked = isLiked;
        r.createdAt = post.getCreatedAt();
        r.author = author;
        r.comments = comments;
        return r;
    }
}
