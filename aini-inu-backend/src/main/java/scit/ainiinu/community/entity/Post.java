package scit.ainiinu.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.community.exception.CommunityErrorCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId;
    // Member 연관관계 대신 ID 참조를 유지해 컨텍스트를 분리한다.

    @Column(nullable = false, length = 2000)
    private String content;

    @ElementCollection
    @CollectionTable(name = "post_image_url", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int commentCount = 0;

    //동시성처리방향
    @Version
    private Long version;

    public static Post create(Long authorId, String content, List<String> imageUrls) {
        Post post = new Post();
        post.authorId = authorId;
        post.update(content, imageUrls);
        return post;
    }


    //게시글 수정
    public void update(String content, List<String> imageUrls) {
        validateContent(content);
        validateImages(imageUrls);
        this.content = content;
        this.imageUrls.clear();
        if (imageUrls != null) this.imageUrls.addAll(imageUrls);
    }

    //글자수 제한 검증
    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new BusinessException(CommunityErrorCode.INVALID_CONTENT_LENGTH);
        }
    }

    //이미지 수 제한 검증
    private void validateImages(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > 5) {
            throw new BusinessException(CommunityErrorCode.EXCEEDED_IMAGE_COUNT);
        }
    }

    //좋아요 +1
    public void increaseLike() {
        this.likeCount++;
    }

    //좋아요 취소
    public void decreaseLike() {
        if (this.likeCount > 0) this.likeCount--;
    }

    //댓글 추가
    public void increaseComment() {
        this.commentCount++;
    }

    //댓글 삭제
    public void decreaseComment() {
        if (this.commentCount > 0) this.commentCount--;
    }
}
