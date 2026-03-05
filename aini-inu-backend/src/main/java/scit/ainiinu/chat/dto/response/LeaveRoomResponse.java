package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveRoomResponse {
    private Long roomId;
    private boolean left;
    private String roomStatus;
}
