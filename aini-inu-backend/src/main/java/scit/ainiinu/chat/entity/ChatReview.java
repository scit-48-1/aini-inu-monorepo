package scit.ainiinu.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_review_room_reviewer_reviewee",
                columnNames = {"chat_room_id", "reviewer_id", "reviewee_id"}
        )
)
public class ChatReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 1000)
    private String comment;

    private ChatReview(Long chatRoomId, Long reviewerId, Long revieweeId, Integer score, String comment) {
        validateScore(score);
        this.chatRoomId = chatRoomId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.score = score;
        this.comment = comment;
    }

    public static ChatReview create(Long chatRoomId, Long reviewerId, Long revieweeId, Integer score, String comment) {
        return new ChatReview(chatRoomId, reviewerId, revieweeId, score, comment);
    }

    private void validateScore(Integer score) {
        if (score == null || score < 1 || score > 5) {
            throw new ChatException(ChatErrorCode.INVALID_REVIEW_SCORE);
        }
    }
}
