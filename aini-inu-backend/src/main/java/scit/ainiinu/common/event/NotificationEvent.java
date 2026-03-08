package scit.ainiinu.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class NotificationEvent {

    private final Long recipientMemberId;
    private final String type;
    private final Map<String, Object> payload;
    private final LocalDateTime occurredAt;

    public static NotificationEvent of(Long recipientId, String type, Map<String, Object> payload) {
        return new NotificationEvent(recipientId, type, payload, LocalDateTime.now());
    }
}
