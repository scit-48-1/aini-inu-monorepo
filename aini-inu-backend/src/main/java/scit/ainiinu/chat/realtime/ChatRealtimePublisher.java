package scit.ainiinu.chat.realtime;

public interface ChatRealtimePublisher {

    void publish(Long chatRoomId, ChatRealtimeEvent<?> event);
}
