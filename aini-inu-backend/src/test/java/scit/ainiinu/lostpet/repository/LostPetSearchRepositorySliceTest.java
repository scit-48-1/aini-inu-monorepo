package scit.ainiinu.lostpet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:lostpet-search-repo;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class LostPetSearchRepositorySliceTest {

    @Autowired
    private LostPetReportRepository lostPetReportRepository;

    @Autowired
    private SightingRepository sightingRepository;

    @Autowired
    private LostPetSearchSessionRepository lostPetSearchSessionRepository;

    @Autowired
    private LostPetSearchCandidateRepository lostPetSearchCandidateRepository;

    @Test
    @DisplayName("세션 기준으로 rank 순 후보를 조회할 수 있다")
    void findCandidatesBySessionOrderByRank() {
        LostPetReport report = lostPetReportRepository.save(LostPetReport.create(
                10L,
                "Momo",
                "Poodle",
                "https://cdn/momo.jpg",
                "desc",
                LocalDateTime.now(),
                "Gangnam"
        ));
        Sighting sightingA = sightingRepository.save(Sighting.create(
                20L,
                "https://cdn/sightingA.jpg",
                LocalDateTime.now(),
                "Yeoksam",
                "memoA"
        ));
        Sighting sightingB = sightingRepository.save(Sighting.create(
                21L,
                "https://cdn/sightingB.jpg",
                LocalDateTime.now(),
                "Jamsil",
                "memoB"
        ));

        LostPetSearchSession session = lostPetSearchSessionRepository.save(
                LostPetSearchSession.create(
                        10L,
                        report,
                        "LOST",
                        "https://cdn/query.jpg",
                        null,
                        LocalDateTime.now().plusHours(24)
                )
        );

        lostPetSearchCandidateRepository.save(LostPetSearchCandidate.create(
                session,
                sightingB,
                new BigDecimal("0.80000"),
                new BigDecimal("0.30000"),
                new BigDecimal("0.60000"),
                new BigDecimal("0.68000"),
                2
        ));
        lostPetSearchCandidateRepository.save(LostPetSearchCandidate.create(
                session,
                sightingA,
                new BigDecimal("0.90000"),
                new BigDecimal("0.40000"),
                new BigDecimal("0.70000"),
                new BigDecimal("0.79000"),
                1
        ));

        Slice<LostPetSearchCandidate> slice = lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(
                session.getId(),
                PageRequest.of(0, 20)
        );

        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.getContent().get(0).getRankOrder()).isEqualTo(1);
        assertThat(slice.getContent().get(1).getRankOrder()).isEqualTo(2);
    }
}
