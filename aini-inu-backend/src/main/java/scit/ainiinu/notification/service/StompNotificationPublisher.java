package scit.ainiinu.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import scit.ainiinu.notification.dto.response.NotificationResponse;

@Service
@RequiredArgsConstructor
public class StompNotificationPublisher implements NotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(Long memberId, NotificationResponse response) {
        messagingTemplate.convertAndSendToUser(
                memberId.toString(),
                "/queue/notifications",
                response
        );
    }
}
