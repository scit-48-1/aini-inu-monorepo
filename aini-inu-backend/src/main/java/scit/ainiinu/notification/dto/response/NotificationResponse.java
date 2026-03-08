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
}
