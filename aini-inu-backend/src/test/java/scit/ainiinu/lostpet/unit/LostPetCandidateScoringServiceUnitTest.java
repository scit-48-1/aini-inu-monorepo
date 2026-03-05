package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scit.ainiinu.lostpet.service.LostPetCandidateScoringService;

class LostPetCandidateScoringServiceUnitTest {

    private final LostPetCandidateScoringService scoringService = new LostPetCandidateScoringService();

    @Test
    @DisplayName("가중치 0.7/0.2/0.1로 총점을 계산한다")
    void computeTotalScore() {
        BigDecimal total = scoringService.computeTotalScore(
                new BigDecimal("0.90000"),
                new BigDecimal("0.50000"),
                new BigDecimal("0.70000")
        );

        assertThat(total).isEqualByComparingTo("0.80000");
    }

    @Test
    @DisplayName("시간 차이가 작을수록 recency 점수가 높다")
    void recencyScore() {
        LocalDateTime base = LocalDateTime.of(2026, 2, 28, 10, 0);
        BigDecimal near = scoringService.computeRecencyScore(base, base.plusHours(3));
        BigDecimal far = scoringService.computeRecencyScore(base, base.plusHours(96));

        assertThat(near).isGreaterThan(far);
        assertThat(far).isEqualByComparingTo("0.00000");
    }
}
