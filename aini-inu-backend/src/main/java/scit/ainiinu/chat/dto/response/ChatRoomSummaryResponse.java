package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomSummaryResponse {
    private Long chatRoomId;
    private String chatType;
    private String status;
    private ChatMessageResponse lastMessage;
    private LocalDateTime updatedAt;
}
