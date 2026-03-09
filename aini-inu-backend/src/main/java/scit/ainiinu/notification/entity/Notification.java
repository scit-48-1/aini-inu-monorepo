package scit.ainiinu.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    private Long referenceId;

    @Column(length = 30)
    private String referenceType;

    @Column(nullable = false)
    private boolean isRead = false;

    public static Notification create(Long recipientMemberId, NotificationType type,
                                       String title, String message,
                                       Long referenceId, String referenceType) {
        Notification notification = new Notification();
        notification.recipientMemberId = recipientMemberId;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.referenceId = referenceId;
        notification.referenceType = referenceType;
        return notification;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
