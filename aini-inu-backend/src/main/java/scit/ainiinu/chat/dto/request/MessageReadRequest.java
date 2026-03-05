package scit.ainiinu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MessageReadRequest {

    @NotNull
    @Schema(description = "messageId 값입니다.", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long messageId;

    @Schema(description = "readAt 값입니다.", example = "2026-03-05T01:20:00Z")
    private OffsetDateTime readAt;
}
