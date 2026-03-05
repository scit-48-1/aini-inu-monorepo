package scit.ainiinu.chat.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatRealtimeEventHandler {

    private final ChatRealtimePublisher chatRealtimePublisher;

    public void publishMessageCreated(Long chatRoomId, ChatMessageResponse message) {
        ChatRealtimeEvent<Map<String, Object>> event = new ChatRealtimeEvent<>(
                "CHAT_MESSAGE_CREATED",
                Map.of(
                        "roomId", chatRoomId,
                        "message", message
                )
        );
        chatRealtimePublisher.publish(chatRoomId, event);
    }

    public void publishMessageDelivered(Long chatRoomId, Long messageId, Long memberId, OffsetDateTime deliveredAt) {
        ChatRealtimeEvent<Map<String, Object>> event = new ChatRealtimeEvent<>(
                "CHAT_MESSAGE_DELIVERED",
                Map.of(
                        "roomId", chatRoomId,
                        "messageId", messageId,
                        "memberId", memberId,
                        "deliveredAt", deliveredAt
                )
        );
        chatRealtimePublisher.publish(chatRoomId, event);
    }

    public void publishMessageRead(Long chatRoomId, Long messageId, Long memberId, OffsetDateTime readAt) {
        ChatRealtimeEvent<Map<String, Object>> event = new ChatRealtimeEvent<>(
                "CHAT_MESSAGE_READ",
                Map.of(
                        "roomId", chatRoomId,
                        "messageId", messageId,
                        "memberId", memberId,
                        "readAt", readAt
                )
        );
        chatRealtimePublisher.publish(chatRoomId, event);
    }
}
