package scit.ainiinu.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import scit.ainiinu.common.event.NotificationEvent;
import scit.ainiinu.notification.dto.response.NotificationResponse;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationPublisher notificationPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        Map<String, Object> payload = event.getPayload();
        NotificationResponse response = new NotificationResponse(
                event.getType(),
                payload,
                event.getOccurredAt(),
                payload.get("notificationId") instanceof Long id ? id : null,
                payload.get("title") instanceof String t ? t : null,
                payload.get("message") instanceof String m ? m : null,
                payload.get("referenceId") instanceof Long refId ? refId : null,
                payload.get("referenceType") instanceof String refType ? refType : null
        );
        notificationPublisher.sendToUser(event.getRecipientMemberId(), response);
    }
}
