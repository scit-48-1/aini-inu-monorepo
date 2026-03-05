package scit.ainiinu.lostpet.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SightingResponse(
        Long sightingId,
        String status,
        LocalDateTime foundAt
) {
}
