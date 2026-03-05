package scit.ainiinu.lostpet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import scit.ainiinu.common.config.JpaConfig;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:lostpet-report-repo;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class LostPetReportRepositorySliceTest {

    @Autowired
    private LostPetReportRepository lostPetReportRepository;

    @Nested
    @DisplayName("실종 신고 조회")
    class LostPetReportQuery {

        @Test
        @DisplayName("ownerId + status 조건으로 Slice 조회가 가능하다")
        void findByOwnerAndStatus() {
            LostPetReport active = LostPetReport.create(
                    10L,
                    "Momo",
                    "Poodle",
                    "https://cdn/momo.jpg",
                    "desc",
                    LocalDateTime.now(),
                    "Gangnam"
            );
            lostPetReportRepository.save(active);

            Slice<LostPetReport> slice = lostPetReportRepository.findByOwnerIdAndStatus(
                    10L,
                    LostPetReportStatus.ACTIVE,
                    PageRequest.of(0, 20)
            );

            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.getContent().get(0).getPetName()).isEqualTo("Momo");
        }
    }
}
