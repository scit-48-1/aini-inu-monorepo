package scit.ainiinu.walk.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "walk_diary")
public class WalkDiary extends BaseTimeEntity {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final int CONTENT_MAX_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "thread_id")
    private Long threadId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    private String content;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "walk_diary_photo_url", joinColumns = @JoinColumn(name = "walk_diary_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "photo_url", nullable = false, length = 1000)
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "walk_date", nullable = false)
    private LocalDate walkDate;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    private WalkDiary(Long memberId, Long threadId, String title, String content, List<String> photoUrls, LocalDate walkDate, Boolean isPublic) {
        validate(title, content, walkDate, photoUrls);
        this.memberId = memberId;
        this.threadId = threadId;
        this.title = title;
        this.content = content;
        this.walkDate = walkDate;
        this.isPublic = isPublic != null ? isPublic : Boolean.TRUE;
        this.photoUrls.addAll(photoUrls != null ? photoUrls : List.of());
    }

    public static WalkDiary create(
            Long memberId,
            Long threadId,
            String title,
            String content,
            List<String> photoUrls,
            LocalDate walkDate,
            Boolean isPublic
    ) {
        return new WalkDiary(memberId, threadId, title, content, photoUrls, walkDate, isPublic);
    }

    public void update(
            Long threadId,
            String title,
            String content,
            List<String> photoUrls,
            LocalDate walkDate,
            Boolean isPublic
    ) {
        if (title != null) {
            if (title.isBlank()) {
                throw new BusinessException(WalkDiaryErrorCode.INVALID_REQUEST);
            }
            this.title = title;
        }
        if (content != null) {
            if (content.isBlank() || content.length() > CONTENT_MAX_LENGTH) {
                throw new BusinessException(WalkDiaryErrorCode.INVALID_REQUEST);
            }
            this.content = content;
        }
        if (walkDate != null) {
            this.walkDate = walkDate;
        }
        if (threadId != null) {
            this.threadId = threadId;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        if (photoUrls != null) {
            if (photoUrls.size() > MAX_IMAGE_COUNT) {
                throw new BusinessException(WalkDiaryErrorCode.IMAGE_COUNT_EXCEEDED);
            }
            this.photoUrls.clear();
            this.photoUrls.addAll(photoUrls);
        }
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    private void validate(String title, String content, LocalDate walkDate, List<String> photoUrls) {
        if (title == null
                || title.isBlank()
                || content == null
                || content.isBlank()
                || content.length() > CONTENT_MAX_LENGTH
                || walkDate == null) {
            throw new BusinessException(WalkDiaryErrorCode.INVALID_REQUEST);
        }
        if (photoUrls != null && photoUrls.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(WalkDiaryErrorCode.IMAGE_COUNT_EXCEEDED);
        }
    }
}
