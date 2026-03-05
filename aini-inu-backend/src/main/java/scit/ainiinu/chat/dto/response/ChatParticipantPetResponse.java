package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatParticipantPetResponse {
    @Schema(description = "반려견 ID입니다.", example = "101")
    private Long petId;
    @Schema(description = "이름입니다.", example = "몽이")
    private String name;
}
