package scit.ainiinu.community.dto;

import lombok.Data;
import scit.ainiinu.community.entity.Comment;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private PostResponse.Author author;
    private String content;
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
