package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomDetailResponse {
    @Schema(description = "채팅방 ID입니다.", example = "101")
    private Long chatRoomId;
    @Schema(description = "채팅 타입 코드입니다.", example = "DIRECT", allowableValues = {"DIRECT", "GROUP"})
    private String chatType;
    @Schema(description = "상태 코드입니다.", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED"})
    private String status;
    @Schema(description = "walkConfirmed 값입니다.", example = "true")
    private boolean walkConfirmed;
    @Schema(description = "participants 값입니다.", example = "[\"예시 항목\"]")
    private List<ChatParticipantResponse> participants;
    @Schema(description = "lastMessage 값입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private ChatMessageResponse lastMessage;
}
