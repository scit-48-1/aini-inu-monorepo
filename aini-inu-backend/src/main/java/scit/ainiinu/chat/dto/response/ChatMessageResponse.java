package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long roomId;
    private ChatSenderResponse sender;
    private String content;
    private String messageType;
    private String status;
    private String clientMessageId;
    private OffsetDateTime sentAt;
}
