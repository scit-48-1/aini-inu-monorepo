package scit.ainiinu.lostpet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class LostPetCandidateScoringService {

    private static final BigDecimal WEIGHT_SIMILARITY = new BigDecimal("0.7");
    private static final BigDecimal WEIGHT_DISTANCE = new BigDecimal("0.2");
    private static final BigDecimal WEIGHT_RECENCY = new BigDecimal("0.1");
    private static final long RECENCY_HOURS_CAP = 72L;

    public BigDecimal normalizeSimilarity(BigDecimal rawSimilarity) {
        if (rawSimilarity == null) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        return clamp01(rawSimilarity);
    }

    public BigDecimal computeDistanceScore(String lostLocation, String foundLocation) {
        if (lostLocation == null || foundLocation == null) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        String normalizedLost = lostLocation.trim().toLowerCase();
        String normalizedFound = foundLocation.trim().toLowerCase();
        if (normalizedLost.equals(normalizedFound)) {
            return new BigDecimal("1.0").setScale(5, RoundingMode.HALF_UP);
        }
        if (normalizedLost.contains(normalizedFound) || normalizedFound.contains(normalizedLost)) {
            return new BigDecimal("0.6").setScale(5, RoundingMode.HALF_UP);
        }
        return new BigDecimal("0.2").setScale(5, RoundingMode.HALF_UP);
    }

    public BigDecimal computeRecencyScore(LocalDateTime lastSeenAt, LocalDateTime foundAt) {
        if (lastSeenAt == null || foundAt == null) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        long diffHours = Math.abs(Duration.between(lastSeenAt, foundAt).toHours());
        long bounded = Math.min(diffHours, RECENCY_HOURS_CAP);
        BigDecimal ratio = BigDecimal.valueOf(RECENCY_HOURS_CAP - bounded)
                .divide(BigDecimal.valueOf(RECENCY_HOURS_CAP), 5, RoundingMode.HALF_UP);
        return clamp01(ratio);
    }

    public BigDecimal computeTotalScore(
            BigDecimal similarityScore,
            BigDecimal distanceScore,
            BigDecimal recencyScore
    ) {
        BigDecimal total = similarityScore.multiply(WEIGHT_SIMILARITY)
                .add(distanceScore.multiply(WEIGHT_DISTANCE))
                .add(recencyScore.multiply(WEIGHT_RECENCY));
        return clamp01(total);
    }

    private BigDecimal clamp01(BigDecimal value) {
        BigDecimal normalized = value.setScale(5, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        if (normalized.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(5, RoundingMode.HALF_UP);
        }
        return normalized;
    }
}
