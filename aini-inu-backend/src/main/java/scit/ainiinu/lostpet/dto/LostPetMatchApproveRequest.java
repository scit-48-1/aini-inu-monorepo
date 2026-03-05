package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LostPetMatchApproveRequest {

    @NotNull
    @Schema(description = "분석 세션 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sessionId;

    @NotNull
    @Schema(description = "sightingId 값입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sightingId;
}
