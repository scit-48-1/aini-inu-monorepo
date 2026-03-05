package scit.ainiinu.walk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "thread_application",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"thread_id", "member_id"})
        }
)
public class WalkThreadApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalkThreadApplicationStatus status;

    @Version
    private Long version;

    private WalkThreadApplication(Long threadId, Long memberId, Long chatRoomId, WalkThreadApplicationStatus status) {
        this.threadId = threadId;
        this.memberId = memberId;
        this.chatRoomId = chatRoomId;
        this.status = status;
    }

    public static WalkThreadApplication joined(Long threadId, Long memberId, Long chatRoomId) {
        return new WalkThreadApplication(threadId, memberId, chatRoomId, WalkThreadApplicationStatus.JOINED);
    }

    public static WalkThreadApplication canceled(Long threadId, Long memberId) {
        return new WalkThreadApplication(threadId, memberId, null, WalkThreadApplicationStatus.CANCELED);
    }

    public void cancel() {
        this.status = WalkThreadApplicationStatus.CANCELED;
    }

    public void rejoin(Long chatRoomId) {
        this.status = WalkThreadApplicationStatus.JOINED;
        this.chatRoomId = chatRoomId;
    }

    public boolean isJoined() {
        return this.status == WalkThreadApplicationStatus.JOINED;
    }
}
