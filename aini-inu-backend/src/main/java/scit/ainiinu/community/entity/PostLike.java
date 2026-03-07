package scit.ainiinu.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "post_like",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_post_like_post_member",
            columnNames = {"post_id", "member_id"}
        )
    }
)
public class PostLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글 ID (FK 제거 -- 삭제 시 cascade 충돌 방지)
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static PostLike create(Long postId, Long memberId) {
        PostLike postLike = new PostLike();
        postLike.postId = postId;
        postLike.memberId = memberId;
        return postLike;
    }
}
