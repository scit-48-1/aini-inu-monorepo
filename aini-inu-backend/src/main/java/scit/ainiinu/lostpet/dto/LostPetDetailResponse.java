package scit.ainiinu.lostpet.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record LostPetDetailResponse(
        Long lostPetId,
        Long ownerId,
        String petName,
        String photoUrl,
        LocalDateTime lastSeenAt,
        String lastSeenLocation,
        String status
) {
}
