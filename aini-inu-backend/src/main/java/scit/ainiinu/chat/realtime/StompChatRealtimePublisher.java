package scit.ainiinu.chat.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompChatRealtimePublisher implements ChatRealtimePublisher {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(Long chatRoomId, ChatRealtimeEvent<?> event) {
        simpMessagingTemplate.convertAndSend("/topic/chat-rooms/" + chatRoomId + "/events", event);
    }
}
