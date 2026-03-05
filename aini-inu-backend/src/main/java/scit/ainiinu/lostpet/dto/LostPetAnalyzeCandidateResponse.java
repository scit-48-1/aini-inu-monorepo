package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record LostPetAnalyzeCandidateResponse(
        @Schema(description = "sightingId 값입니다.", example = "101")
        Long sightingId,
        @Schema(description = "finderId 값입니다.", example = "101")
        Long finderId,
        @Schema(description = "scoreSimilarity 값입니다.", example = "4.5")
        BigDecimal scoreSimilarity,
        @Schema(description = "scoreDistance 값입니다.", example = "4.5")
        BigDecimal scoreDistance,
        @Schema(description = "scoreRecency 값입니다.", example = "4.5")
        BigDecimal scoreRecency,
        @Schema(description = "scoreTotal 값입니다.", example = "4.5")
        BigDecimal scoreTotal,
        @Schema(description = "rank 값입니다.", example = "20")
        Integer rank,
        @Schema(description = "상태 코드입니다.", example = "CANDIDATE", allowableValues = {"CANDIDATE", "APPROVED"})
        String status
) {
}
