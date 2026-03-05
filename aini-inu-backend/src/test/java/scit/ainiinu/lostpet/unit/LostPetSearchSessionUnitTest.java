package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;

class LostPetSearchSessionUnitTest {

    @Test
    @DisplayName("세션 만료 시각 이후에는 만료로 판단한다")
    void isExpired() {
        LostPetReport report = LostPetReport.create(
                10L,
                "Momo",
                "Poodle",
                "https://cdn/momo.jpg",
                "desc",
                LocalDateTime.of(2026, 2, 28, 10, 0),
                "Gangnam"
        );
        LostPetSearchSession session = LostPetSearchSession.create(
                10L,
                report,
                "LOST",
                "https://cdn/query.jpg",
                null,
                LocalDateTime.of(2026, 2, 28, 12, 0)
        );

        assertThat(session.isExpired(LocalDateTime.of(2026, 2, 28, 11, 59))).isFalse();
        assertThat(session.isExpired(LocalDateTime.of(2026, 2, 28, 12, 0))).isTrue();
    }
}
