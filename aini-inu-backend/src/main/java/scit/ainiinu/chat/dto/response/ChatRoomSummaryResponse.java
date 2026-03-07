package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatRoomSummaryResponse {
    @Schema(description = "채팅방 ID입니다.", example = "101")
    private Long chatRoomId;
    @Schema(description = "채팅 타입 코드입니다.", example = "DIRECT", allowableValues = {"DIRECT", "GROUP"})
    private String chatType;
    @Schema(description = "상태 코드입니다.", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED"})
    private String status;
    @Schema(description = "출처 코드입니다.", example = "DM", allowableValues = {"DM", "WALK", "LOST_PET"})
    private String origin;
    @Schema(description = "채팅방 제목입니다 (산책/실종 채팅방용).", example = "한강 저녁 산책")
    private String roomTitle;
    @Schema(description = "채팅방 표시 이름입니다 (상대방 닉네임).", example = "홍길동")
    private String displayName;
    @Schema(description = "lastMessage 값입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private ChatMessageResponse lastMessage;
    @Schema(description = "참여자 프로필 이미지 목록입니다 (본인 제외, 최대 4명).", example = "[\"https://example.com/profile.jpg\"]")
    private List<String> participantProfileImages;
    @Schema(description = "수정 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime updatedAt;
}
