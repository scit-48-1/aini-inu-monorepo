package scit.ainiinu.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.community.exception.CommunityErrorCode;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글 ID (FK 제거 -- 삭제 시 cascade 충돌 방지)
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // 작성자 ID (회원 컨텍스트 분리를 위해 ID 참조 유지)
    @Column(nullable = false)
    private Long authorId;

    // 댓글 내용 (최대 500자)
    @Column(nullable = false, length = 500)
    private String content;

    // 생성 메서드
    public static Comment create(Long postId, Long authorId, String content) {
        validateContent(content);
        Comment comment = new Comment();
        comment.postId = postId;
        comment.authorId = authorId;
        comment.content = content;
        return comment;
    }

    private static void validateContent(String content) {
        if (content == null || content.length() > 500) {
            throw new BusinessException(CommunityErrorCode.INVALID_CONTENT_LENGTH);
        }
    }
}
