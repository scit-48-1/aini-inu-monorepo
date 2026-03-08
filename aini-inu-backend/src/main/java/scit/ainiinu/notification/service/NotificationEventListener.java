package scit.ainiinu.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import scit.ainiinu.common.event.NotificationEvent;
import scit.ainiinu.notification.dto.response.NotificationResponse;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationPublisher notificationPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        NotificationResponse response = new NotificationResponse(
                event.getType(),
                event.getPayload(),
                event.getOccurredAt()
        );
        notificationPublisher.sendToUser(event.getRecipientMemberId(), response);
    }
}
