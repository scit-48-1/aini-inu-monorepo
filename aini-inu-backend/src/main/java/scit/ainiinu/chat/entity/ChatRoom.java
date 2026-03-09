package scit.ainiinu.chat.entity;

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

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room")
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id")
    private Long threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type", nullable = false, length = 20)
    private ChatRoomType chatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomOrigin origin;

    @Column(name = "room_title", length = 200)
    private String roomTitle;

    @Column(name = "walk_confirmed", nullable = false)
    private Boolean walkConfirmed;

    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Version
    private Long version;

    @Builder
    private ChatRoom(Long threadId, ChatRoomType chatType, ChatRoomStatus status,
                     ChatRoomOrigin origin, String roomTitle, Boolean walkConfirmed) {
        this.threadId = threadId;
        this.chatType = chatType;
        this.status = status != null ? status : ChatRoomStatus.ACTIVE;
        this.origin = origin != null ? origin : ChatRoomOrigin.DM;
        this.roomTitle = roomTitle;
        this.walkConfirmed = walkConfirmed != null ? walkConfirmed : Boolean.FALSE;
    }

    public static ChatRoom create(Long threadId, ChatRoomType chatType, ChatRoomStatus status,
                                  ChatRoomOrigin origin, String roomTitle) {
        return ChatRoom.builder()
                .threadId(threadId)
                .chatType(chatType)
                .status(status)
                .origin(origin)
                .roomTitle(roomTitle)
                .walkConfirmed(false)
                .build();
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }

    public void reopen() {
        this.status = ChatRoomStatus.ACTIVE;
    }

    public void updateWalkConfirmed(boolean walkConfirmed) {
        this.walkConfirmed = walkConfirmed;
    }

    public boolean isClosed() {
        return this.status == ChatRoomStatus.CLOSED;
    }
}
