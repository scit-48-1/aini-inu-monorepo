package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WalkConfirmResponse {
    @Schema(description = "roomId 값입니다.", example = "101")
    private Long roomId;
    @Schema(description = "회원 ID입니다.", example = "101")
    private Long memberId;
    @Schema(description = "myState 값입니다.", example = "예시 문자열")
    private String myState;
    @Schema(description = "allConfirmed 값입니다.", example = "true")
    private boolean allConfirmed;
    @Schema(description = "confirmedMemberIds 값입니다.", example = "[101,102]")
    private List<Long> confirmedMemberIds;
}
