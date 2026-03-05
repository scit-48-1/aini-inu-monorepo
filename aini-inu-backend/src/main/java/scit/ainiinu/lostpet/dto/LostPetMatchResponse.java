package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;

@Builder
public record LostPetMatchResponse(
        @Schema(description = "matchId 값입니다.", example = "101")
        Long matchId,
        @Schema(description = "상태 코드입니다.", example = "PENDING_APPROVAL", allowableValues = {"PENDING_APPROVAL", "APPROVED", "PENDING_CHAT_LINK", "CHAT_LINKED", "REJECTED", "INVALIDATED"})
        String status,
        @Schema(description = "채팅방 ID입니다.", example = "101")
        Long chatRoomId
) {
}
