package scit.ainiinu.member.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class WalkStatsResponse {
    private int windowDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private String timezone;
    private int totalWalks;
    private List<WalkStatsPointResponse> points;
}
