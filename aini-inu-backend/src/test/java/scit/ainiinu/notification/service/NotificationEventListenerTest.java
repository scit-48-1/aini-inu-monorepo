package scit.ainiinu.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.common.event.NotificationEvent;
import scit.ainiinu.notification.dto.response.NotificationResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("NotificationEvent 수신 시 NotificationPublisher.sendToUser()가 호출된다")
    void handleNotificationEvent_callsSendToUser() {
        // given
        Long recipientId = 42L;
        String type = "CHAT_NEW_MESSAGE";
        Map<String, Object> payload = Map.of("roomId", 10L, "senderMemberId", 1L);
        NotificationEvent event = NotificationEvent.of(recipientId, type, payload);

        // when
        notificationEventListener.handleNotificationEvent(event);

        // then
        ArgumentCaptor<NotificationResponse> captor = ArgumentCaptor.forClass(NotificationResponse.class);
        then(notificationPublisher).should().sendToUser(eq(recipientId), captor.capture());

        NotificationResponse response = captor.getValue();
        assertThat(response.getType()).isEqualTo("CHAT_NEW_MESSAGE");
        assertThat(response.getPayload()).containsEntry("roomId", 10L);
        assertThat(response.getPayload()).containsEntry("senderMemberId", 1L);
        assertThat(response.getOccurredAt()).isEqualTo(event.getOccurredAt());
    }
}
