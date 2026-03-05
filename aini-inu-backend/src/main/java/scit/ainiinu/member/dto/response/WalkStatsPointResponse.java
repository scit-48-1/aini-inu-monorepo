package scit.ainiinu.member.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WalkStatsPointResponse {
    private LocalDate date;
    private int count;
}
