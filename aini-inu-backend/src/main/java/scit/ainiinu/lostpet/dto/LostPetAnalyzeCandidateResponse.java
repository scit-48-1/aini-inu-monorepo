package scit.ainiinu.lostpet.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record LostPetAnalyzeCandidateResponse(
        Long sightingId,
        Long finderId,
        BigDecimal scoreSimilarity,
        BigDecimal scoreDistance,
        BigDecimal scoreRecency,
        BigDecimal scoreTotal,
        Integer rank,
        String status
) {
}
