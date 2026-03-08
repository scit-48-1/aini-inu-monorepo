package scit.ainiinu.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import scit.ainiinu.notification.dto.response.NotificationResponse;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StompNotificationPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private StompNotificationPublisher stompNotificationPublisher;

    @Test
    @DisplayName("sendToUser 호출 시 SimpMessagingTemplate.convertAndSendToUser()가 올바른 인자로 호출된다")
    void sendToUser_callsConvertAndSendToUser() {
        // given
        Long memberId = 42L;
        NotificationResponse response = new NotificationResponse(
                "CHAT_NEW_MESSAGE",
                Map.of("roomId", 10L),
                LocalDateTime.now()
        );

        // when
        stompNotificationPublisher.sendToUser(memberId, response);

        // then
        then(messagingTemplate).should().convertAndSendToUser(
                "42",
                "/queue/notifications",
                response
        );
    }
}
