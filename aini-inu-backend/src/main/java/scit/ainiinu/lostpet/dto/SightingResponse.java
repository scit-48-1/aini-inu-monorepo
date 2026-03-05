package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record SightingResponse(
        @Schema(description = "sightingId 값입니다.", example = "101")
        Long sightingId,
        @Schema(description = "상태 코드입니다.", example = "OPEN", allowableValues = {"OPEN", "CLOSED"})
        String status,
        @Schema(description = "foundAt 값입니다.", example = "2026-03-05T01:20:00Z")
        LocalDateTime foundAt
) {
}
