package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatParticipantResponse {
    @Schema(description = "회원 ID입니다.", example = "101")
    private Long memberId;
    @Schema(description = "참여자의 산책 확정 상태입니다.", example = "UNCONFIRMED", allowableValues = {"UNCONFIRMED", "CONFIRMED"})
    private String walkConfirmState;
    @Schema(description = "left 값입니다.", example = "true")
    private boolean left;
    @Schema(description = "pets 값입니다.", example = "[\"예시 항목\"]")
    private List<ChatParticipantPetResponse> pets;
}
