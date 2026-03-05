package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveRoomResponse {
    @Schema(description = "roomId 값입니다.", example = "101")
    private Long roomId;
    @Schema(description = "left 값입니다.", example = "true")
    private boolean left;
    @Schema(description = "채팅방 상태 코드입니다.", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED"})
    private String roomStatus;
}
