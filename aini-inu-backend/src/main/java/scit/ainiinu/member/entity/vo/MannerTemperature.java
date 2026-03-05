package scit.ainiinu.member.entity.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class MannerTemperature {
    private static final BigDecimal MIN = BigDecimal.ONE;
    private static final BigDecimal MAX = BigDecimal.TEN;
    private static final BigDecimal DEFAULT = new BigDecimal("5.0");

    private BigDecimal value;

    public MannerTemperature(BigDecimal value) {
        this.value = value;
    }

    public static MannerTemperature fromAverage(int sum, int count) {
        if (count <= 0) {
            return new MannerTemperature(DEFAULT);
        }
        BigDecimal avg = BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
        if (avg.compareTo(MIN) < 0) avg = MIN;
        if (avg.compareTo(MAX) > 0) avg = MAX;
        return new MannerTemperature(avg);
    }
}
