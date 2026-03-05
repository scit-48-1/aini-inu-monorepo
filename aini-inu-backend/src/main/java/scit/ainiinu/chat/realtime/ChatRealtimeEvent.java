package scit.ainiinu.chat.realtime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRealtimeEvent<T> {
    private String type;
    private T data;
}
