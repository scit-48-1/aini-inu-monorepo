package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class MessageReadResponse {
    private Long roomId;
    private Long memberId;
    private Long lastReadMessageId;
    private OffsetDateTime updatedAt;
}
