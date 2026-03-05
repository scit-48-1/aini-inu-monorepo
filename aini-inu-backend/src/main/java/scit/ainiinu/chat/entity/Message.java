package scit.ainiinu.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.common.entity.BaseTimeEntity;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "message",
        indexes = {
                @Index(name = "idx_message_room_id_id", columnList = "chat_room_id,id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_room_sender_client",
                        columnNames = {"chat_room_id", "sender_id", "client_message_id"}
                )
        }
)
public class Message extends BaseTimeEntity {

    private static final int MAX_CONTENT_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    @Column(name = "client_message_id", length = 100)
    private String clientMessageId;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    private Message(Long chatRoomId, Long senderId, String content, ChatMessageType messageType, String clientMessageId) {
        validateContent(content);
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType != null ? messageType : ChatMessageType.USER;
        this.clientMessageId = clientMessageId;
        this.sentAt = OffsetDateTime.now();
    }

    public static Message create(Long chatRoomId, Long senderId, String content, ChatMessageType messageType, String clientMessageId) {
        return new Message(chatRoomId, senderId, content, messageType, clientMessageId);
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new ChatException(ChatErrorCode.INVALID_MESSAGE_CONTENT);
        }
    }
}
