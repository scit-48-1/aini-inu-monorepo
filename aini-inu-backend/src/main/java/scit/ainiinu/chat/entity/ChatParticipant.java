package scit.ainiinu.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_participant_room_member",
                columnNames = {"chat_room_id", "member_id"}
        )
)
public class ChatParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "walk_confirm_state", nullable = false, length = 20)
    private ChatWalkConfirmState walkConfirmState;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    private ChatParticipant(Long chatRoomId, Long memberId) {
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.joinedAt = LocalDateTime.now();
        this.walkConfirmState = ChatWalkConfirmState.UNCONFIRMED;
    }

    public static ChatParticipant create(Long chatRoomId, Long memberId) {
        return new ChatParticipant(chatRoomId, memberId);
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public void rejoin() {
        this.leftAt = null;
        this.joinedAt = LocalDateTime.now();
        this.walkConfirmState = ChatWalkConfirmState.UNCONFIRMED;
    }

    public void confirmWalk() {
        this.walkConfirmState = ChatWalkConfirmState.CONFIRMED;
    }

    public void cancelWalkConfirm() {
        this.walkConfirmState = ChatWalkConfirmState.UNCONFIRMED;
    }

    public void markRead(Long messageId) {
        if (messageId == null) {
            return;
        }
        if (this.lastReadMessageId == null || this.lastReadMessageId < messageId) {
            this.lastReadMessageId = messageId;
        }
    }

    public boolean isLeft() {
        return this.leftAt != null;
    }
}
