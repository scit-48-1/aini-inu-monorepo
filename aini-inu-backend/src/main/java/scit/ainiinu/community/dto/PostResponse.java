package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.Getter;
import scit.ainiinu.community.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "author 값입니다.", example = "예시 문자열")
    private Author author; //작성자 정보 묶음
    @Schema(description = "본문 내용입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private String content;
    @Schema(description = "이미지 URL 목록입니다.", example = "[\"https://cdn.example.com/sample.jpg\"]")
    private List<String> imageUrls;
    @Schema(description = "likeCount 값입니다.", example = "20")
    private int likeCount;
    @Schema(description = "commentCount 값입니다.", example = "20")
    private int commentCount;
    @Schema(description = "isLiked 값입니다.", example = "true")
    private boolean isLiked;
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
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
        @Schema(description = "리소스 식별자입니다.", example = "101")
        private Long id;
        @Schema(description = "닉네임입니다.", example = "몽이아빠")
        private String nickname;
        @Schema(description = "프로필 이미지 URL입니다.", example = "https://cdn.example.com/sample.jpg")
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
