package scit.ainiinu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageCreateRequest {

    @Schema(
            description = "전송할 메시지 본문입니다.",
            example = "오늘 저녁 7시에 한강공원에서 만나요.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    @NotBlank
    @Size(min = 1, max = 500)
    private String content;

    @Schema(description = "메시지 타입 코드입니다.", example = "USER", allowableValues = {"USER", "SYSTEM"})
    private String messageType;

    @Schema(description = "클라이언트 메시지 멱등 키입니다. 동일 채팅방/발신자에서 중복 전송 방지에 사용합니다.", example = "msg-20260305-0001")
    private String clientMessageId;
}
