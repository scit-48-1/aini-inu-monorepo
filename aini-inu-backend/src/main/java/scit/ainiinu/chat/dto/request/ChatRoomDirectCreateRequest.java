package scit.ainiinu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRoomDirectCreateRequest {

    @NotNull
    @Schema(description = "partnerId 값입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long partnerId;
}
