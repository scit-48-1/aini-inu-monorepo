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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank
    @Size(min = 1, max = 500)
    private String content;

    private String messageType;

    private String clientMessageId;
}
