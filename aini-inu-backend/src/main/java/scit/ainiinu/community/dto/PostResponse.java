package scit.ainiinu.community.dto;

import lombok.Data;
import lombok.Getter;
import scit.ainiinu.community.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private Long id;
    private Author author; //작성자 정보 묶음
    private String content;
    private List<String> imageUrls;
    private int likeCount;
    private int commentCount;
    private boolean isLiked;
    private LocalDateTime createdAt;

    /**
     * 좋아요 여부를 포함하여 PostResponse 생성
     * @param post 게시글 엔티티
     * @param author 작성자 정보
     * @param isLiked 현재 사용자의 좋아요 여부
     * @return PostResponse
     */
    public static PostResponse from(Post post, Author author, boolean isLiked) {
        PostResponse r = new PostResponse();
        r.id = post.getId();
        r.content = post.getContent();
        r.imageUrls = post.getImageUrls();
        r.likeCount = post.getLikeCount();
        r.commentCount = post.getCommentCount();
        r.isLiked = isLiked;
        r.createdAt = post.getCreatedAt();
        r.author = author;
        return r;
    }

    //작성자정보
    @Getter
    public static class Author{
        private Long id;
        private String nickname;
        private String profileImageUrl;

        public static Author of(Long memberId, String nickname, String profileImageUrl){
            Author a = new Author();
            a.id = memberId;
            a.nickname = nickname;
            a.profileImageUrl = profileImageUrl;
            return a;
        }
    }
}
