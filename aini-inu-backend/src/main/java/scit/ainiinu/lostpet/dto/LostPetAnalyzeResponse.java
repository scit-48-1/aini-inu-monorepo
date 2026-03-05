package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import lombok.Builder;

@Builder
public record LostPetAnalyzeResponse(
        @Schema(description = "분석 세션 ID입니다.", example = "101")
        Long sessionId,
        @Schema(description = "summary 값입니다.", example = "예시 문자열")
        String summary,
        @Schema(description = "candidates 값입니다.", example = "[101,102]")
        List<LostPetAnalyzeCandidateResponse> candidates
) {
}
