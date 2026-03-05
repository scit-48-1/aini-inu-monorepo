package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSenderResponse {
    @Schema(description = "회원 ID입니다.", example = "101")
    private Long memberId;

    public static ChatSenderResponse of(Long memberId) {
        return ChatSenderResponse.builder()
                .memberId(memberId)
                .build();
    }
}
