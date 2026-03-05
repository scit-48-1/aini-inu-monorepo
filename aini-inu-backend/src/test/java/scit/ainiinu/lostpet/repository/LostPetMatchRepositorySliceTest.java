package scit.ainiinu.lostpet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.Sighting;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:lostpet-match-repo;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class LostPetMatchRepositorySliceTest {

    @Autowired
    private LostPetReportRepository lostPetReportRepository;

    @Autowired
    private SightingRepository sightingRepository;

    @Autowired
    private LostPetMatchRepository lostPetMatchRepository;

    @Test
    @DisplayName("실종 신고 ID로 매치 목록을 조회할 수 있다")
    void findByLostPetReportId() {
        LostPetReport report = lostPetReportRepository.save(LostPetReport.create(
                10L,
                "Momo",
                "Poodle",
                "https://cdn/momo.jpg",
                "desc",
                LocalDateTime.now(),
                "Gangnam"
        ));
        Sighting sighting = sightingRepository.save(Sighting.create(
                20L,
                "https://cdn/sighting.jpg",
                LocalDateTime.now(),
                "Yeoksam",
                "memo"
        ));

        LostPetMatch match = LostPetMatch.create(report, sighting, new BigDecimal("0.93"));
        lostPetMatchRepository.save(match);

        List<LostPetMatch> matches = lostPetMatchRepository.findByLostPetReportId(report.getId());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getSimilarityTotal()).isEqualByComparingTo("0.93");
    }
}
