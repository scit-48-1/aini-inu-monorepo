package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import scit.ainiinu.community.entity.Comment;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "author 값입니다.", example = "예시 문자열")
    private PostResponse.Author author;
    @Schema(description = "본문 내용입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private String content;
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime createdAt;

    public static CommentResponse from(Comment comment, PostResponse.Author author) {
        CommentResponse r = new CommentResponse();
        r.id = comment.getId();
        r.content = comment.getContent();
        r.createdAt = comment.getCreatedAt();
        r.author = author;
        return r;
    }
}
