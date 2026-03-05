package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSenderResponse {
    private Long memberId;

    public static ChatSenderResponse of(Long memberId) {
        return ChatSenderResponse.builder()
                .memberId(memberId)
                .build();
    }
}
