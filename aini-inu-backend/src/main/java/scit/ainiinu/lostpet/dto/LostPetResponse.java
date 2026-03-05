package scit.ainiinu.lostpet.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record LostPetResponse(
        Long lostPetId,
        String status,
        LocalDateTime createdAt
) {
}
