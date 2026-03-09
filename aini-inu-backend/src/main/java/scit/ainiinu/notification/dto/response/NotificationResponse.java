package scit.ainiinu.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private final String type;
    private final Map<String, Object> payload;
    private final LocalDateTime occurredAt;
    private final Long notificationId;
    private final String title;
    private final String message;
    private final Long referenceId;
    private final String referenceType;

    public NotificationResponse(String type, Map<String, Object> payload, LocalDateTime occurredAt) {
        this(type, payload, occurredAt, null, null, null, null, null);
    }
}
