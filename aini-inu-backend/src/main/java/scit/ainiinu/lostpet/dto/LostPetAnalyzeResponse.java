package scit.ainiinu.lostpet.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record LostPetAnalyzeResponse(
        Long sessionId,
        String summary,
        List<LostPetAnalyzeCandidateResponse> candidates
) {
}
