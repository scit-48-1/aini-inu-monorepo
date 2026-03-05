package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record LostPetSummaryResponse(
        @Schema(description = "실종 신고 ID입니다.", example = "101")
        Long lostPetId,
        @Schema(description = "petName 값입니다.", example = "몽이")
        String petName,
        @Schema(description = "상태 코드입니다.", example = "ACTIVE", allowableValues = {"ACTIVE", "RESOLVED", "CLOSED"})
        String status,
        @Schema(description = "lastSeenAt 값입니다.", example = "2026-03-05T01:20:00Z")
        LocalDateTime lastSeenAt
) {
}
