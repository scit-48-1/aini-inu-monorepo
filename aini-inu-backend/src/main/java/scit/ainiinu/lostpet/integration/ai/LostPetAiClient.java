package scit.ainiinu.lostpet.integration.ai;

import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.domain.Sighting;

public interface LostPetAiClient {
    LostPetAiResult analyze(LostPetAnalyzeRequest request);

    default void indexSighting(Sighting sighting) {
        // no-op fallback
    }
}
