package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class WalkStatsResponse {
    @Schema(description = "집계 윈도우 일수입니다.", example = "20")
    private int windowDays;
    @Schema(description = "startDate 값입니다.", example = "2026-03-05")
    private LocalDate startDate;
    @Schema(description = "endDate 값입니다.", example = "2026-03-05")
    private LocalDate endDate;
    @Schema(description = "시간대 식별자입니다.", example = "Asia/Seoul")
    private String timezone;
    @Schema(description = "총 산책 횟수입니다.", example = "101")
    private int totalWalks;
    @Schema(description = "일자별 집계 포인트 목록입니다.", example = "[\"예시 항목\"]")
    private List<WalkStatsPointResponse> points;
}
