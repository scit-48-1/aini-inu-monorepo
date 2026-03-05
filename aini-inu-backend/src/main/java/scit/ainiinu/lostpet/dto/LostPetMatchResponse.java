package scit.ainiinu.lostpet.dto;

import lombok.Builder;

@Builder
public record LostPetMatchResponse(
        Long matchId,
        String status,
        Long chatRoomId
) {
}
