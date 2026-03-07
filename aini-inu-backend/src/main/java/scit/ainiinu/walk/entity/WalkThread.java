package scit.ainiinu.walk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.walk.exception.ThreadErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "thread")
public class WalkThread extends BaseTimeEntity {

    private static final int TITLE_MAX_LENGTH = 30;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "walk_date", nullable = false)
    private LocalDate walkDate;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type", nullable = false, length = 20)
    private WalkChatType chatType;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "allow_non_pet_owner", nullable = false)
    private Boolean allowNonPetOwner;

    @Column(name = "is_visible_always", nullable = false)
    private Boolean isVisibleAlways;

    @Column(name = "place_name", nullable = false, length = 200)
    private String placeName;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 6)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalkThreadStatus status;

    @Version
    private Long version;

    @Builder
    private WalkThread(
            Long authorId,
            String title,
            String description,
            LocalDate walkDate,
            LocalDateTime startTime,
            LocalDateTime endTime,
            WalkChatType chatType,
            Integer maxParticipants,
            Boolean allowNonPetOwner,
            Boolean isVisibleAlways,
            String placeName,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            WalkThreadStatus status
    ) {
        validateTitle(title);
        validateDescription(description);
        validateTime(startTime, endTime);
        validateParticipants(chatType, maxParticipants);

        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.walkDate = walkDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.chatType = chatType;
        this.maxParticipants = resolveParticipants(chatType, maxParticipants);
        this.allowNonPetOwner = allowNonPetOwner != null ? allowNonPetOwner : Boolean.FALSE;
        this.isVisibleAlways = isVisibleAlways != null ? isVisibleAlways : Boolean.TRUE;
        this.placeName = placeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = status != null ? status : WalkThreadStatus.RECRUITING;
    }

    public void update(
            String title,
            String description,
            LocalDate walkDate,
            LocalDateTime startTime,
            LocalDateTime endTime,
            WalkChatType chatType,
            Integer maxParticipants,
            String placeName,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            Boolean allowNonPetOwner,
            Boolean isVisibleAlways
    ) {
        WalkChatType nextChatType = chatType != null ? chatType : this.chatType;
        Integer nextMaxParticipants = maxParticipants != null ? maxParticipants : this.maxParticipants;

        if (title != null) {
            validateTitle(title);
            this.title = title;
        }
        if (description != null) {
            validateDescription(description);
            this.description = description;
        }
        if (walkDate != null) {
            this.walkDate = walkDate;
        }

        LocalDateTime nextStartTime = startTime != null ? startTime : this.startTime;
        LocalDateTime nextEndTime = endTime != null ? endTime : this.endTime;
        validateTime(nextStartTime, nextEndTime);

        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }

        validateParticipants(nextChatType, nextMaxParticipants);
        this.chatType = nextChatType;
        this.maxParticipants = resolveParticipants(nextChatType, nextMaxParticipants);

        if (placeName != null) {
            this.placeName = placeName;
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
        if (address != null) {
            this.address = address;
        }
        if (allowNonPetOwner != null) {
            this.allowNonPetOwner = allowNonPetOwner;
        }
        if (isVisibleAlways != null) {
            this.isVisibleAlways = isVisibleAlways;
        }
    }

    public void markDeleted() {
        this.status = WalkThreadStatus.DELETED;
    }

    public void expire() {
        if (this.status == WalkThreadStatus.RECRUITING) {
            this.status = WalkThreadStatus.EXPIRED;
        }
    }

    public void complete() {
        if (this.status == WalkThreadStatus.DELETED) {
            return;
        }
        this.status = WalkThreadStatus.COMPLETED;
    }

    public boolean isRecruiting() {
        return this.status == WalkThreadStatus.RECRUITING;
    }

    public boolean isAuthor(Long memberId) {
        return this.authorId.equals(memberId);
    }

    public boolean isExpired(LocalDateTime now) {
        if (this.status == WalkThreadStatus.EXPIRED || this.status == WalkThreadStatus.COMPLETED || this.status == WalkThreadStatus.DELETED) {
            return true;
        }
        return !this.startTime.plusMinutes(60).isAfter(now);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank() || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
    }

    private void validateParticipants(WalkChatType chatType, Integer maxParticipants) {
        if (chatType == null) {
            throw new BusinessException(ThreadErrorCode.INVALID_CHAT_TYPE);
        }
        if (maxParticipants == null) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
        if (chatType == WalkChatType.GROUP && (maxParticipants < 3 || maxParticipants > 10)) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
        if (chatType == WalkChatType.INDIVIDUAL && maxParticipants != 2) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
    }

    private Integer resolveParticipants(WalkChatType chatType, Integer maxParticipants) {
        if (chatType == WalkChatType.INDIVIDUAL) {
            return 2;
        }
        return maxParticipants;
    }
}
