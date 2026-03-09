package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ActivityStatsPointResponse {
    @Schema(description = "date 값입니다.", example = "2026-03-05")
    private LocalDate date;
    @Schema(description = "개수입니다.", example = "20")
    private int count;
}
