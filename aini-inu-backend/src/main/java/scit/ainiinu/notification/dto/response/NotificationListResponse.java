package scit.ainiinu.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import scit.ainiinu.notification.entity.Notification;
import scit.ainiinu.notification.entity.NotificationType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationListResponse {

    private final Long id;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final String referenceType;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    public static NotificationListResponse from(Notification notification) {
        return new NotificationListResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getReferenceType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
