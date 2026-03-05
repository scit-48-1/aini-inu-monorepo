package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.dto.LostPetCreateRequest;
import scit.ainiinu.lostpet.dto.LostPetResponse;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.service.LostPetServiceImpl;

@ExtendWith(MockitoExtension.class)
class LostPetServiceUnitTest {

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @InjectMocks
    private LostPetServiceImpl lostPetService;

    @Nested
    @DisplayName("실종 신고 생성")
    class CreateLostPet {

        @Test
        @DisplayName("활성 중복 신고가 없으면 생성된다")
        void createSuccess() {
            LostPetCreateRequest request = LostPetCreateRequest.builder()
                    .petName("Momo")
                    .breed("Poodle")
                    .photoUrl("https://cdn/momo.jpg")
                    .description("desc")
                    .lastSeenAt(LocalDateTime.now())
                    .lastSeenLocation("Gangnam")
                    .build();
            given(lostPetReportRepository.findActiveDuplicate(10L, "Momo", "Poodle")).willReturn(Optional.empty());
            given(lostPetReportRepository.save(any(LostPetReport.class))).willAnswer(invocation -> {
                LostPetReport report = invocation.getArgument(0);
                report.assignIdForTest(1L);
                return report;
            });

            LostPetResponse response = lostPetService.create(10L, request);

            assertThat(response.lostPetId()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("활성 중복 신고가 있으면 예외를 던진다")
        void duplicateBlocked() {
            LostPetCreateRequest request = LostPetCreateRequest.builder()
                    .petName("Momo")
                    .breed("Poodle")
                    .photoUrl("https://cdn/momo.jpg")
                    .description("desc")
                    .lastSeenAt(LocalDateTime.now())
                    .lastSeenLocation("Gangnam")
                    .build();
            given(lostPetReportRepository.findActiveDuplicate(10L, "Momo", "Poodle"))
                    .willReturn(Optional.of(LostPetReport.create(
                            10L,
                            "Momo",
                            "Poodle",
                            "https://cdn/momo.jpg",
                            "desc",
                            LocalDateTime.now(),
                            "Gangnam"
                    )));

            assertThatThrownBy(() -> lostPetService.create(10L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
