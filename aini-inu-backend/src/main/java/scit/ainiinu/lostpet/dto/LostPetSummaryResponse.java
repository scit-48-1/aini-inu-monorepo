package scit.ainiinu.lostpet.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record LostPetSummaryResponse(
        Long lostPetId,
        String petName,
        String status,
        LocalDateTime lastSeenAt
) {
}
