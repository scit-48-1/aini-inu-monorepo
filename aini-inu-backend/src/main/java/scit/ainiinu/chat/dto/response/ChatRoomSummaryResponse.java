package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomSummaryResponse {
    @Schema(description = "채팅방 ID입니다.", example = "101")
    private Long chatRoomId;
    @Schema(description = "채팅 타입 코드입니다.", example = "DIRECT", allowableValues = {"DIRECT", "GROUP"})
    private String chatType;
    @Schema(description = "상태 코드입니다.", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED"})
    private String status;
    @Schema(description = "lastMessage 값입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private ChatMessageResponse lastMessage;
    @Schema(description = "수정 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime updatedAt;
}
