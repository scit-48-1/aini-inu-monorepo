package scit.ainiinu.lostpet.integration.ai;

import java.math.BigDecimal;

public record LostPetAiCandidate(
        Long sightingId,
        Long finderId,
        BigDecimal similarityTotal
) {
}
