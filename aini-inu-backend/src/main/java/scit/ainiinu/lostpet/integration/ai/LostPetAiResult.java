package scit.ainiinu.lostpet.integration.ai;

import java.util.List;

public record LostPetAiResult(
        String summary,
        List<LostPetAiCandidate> candidates
) {
}
