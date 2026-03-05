package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WalkConfirmResponse {
    private Long roomId;
    private Long memberId;
    private String myState;
    private boolean allConfirmed;
    private List<Long> confirmedMemberIds;
}
