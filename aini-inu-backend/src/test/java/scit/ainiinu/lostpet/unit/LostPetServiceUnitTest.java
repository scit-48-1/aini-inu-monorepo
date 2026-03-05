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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.dto.LostPetCreateRequest;
import scit.ainiinu.lostpet.dto.LostPetDetailResponse;
import scit.ainiinu.lostpet.dto.LostPetResponse;
import scit.ainiinu.lostpet.dto.LostPetSummaryResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetReportQueryRepository;
import scit.ainiinu.lostpet.service.LostPetServiceImpl;

@ExtendWith(MockitoExtension.class)
class LostPetServiceUnitTest {

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @Mock
    private LostPetReportQueryRepository lostPetReportQueryRepository;

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

    @Nested
    @DisplayName("실종 신고 목록 조회")
    class ListLostPet {

        @Test
        @DisplayName("상태를 생략하면 ACTIVE 기준으로 목록을 반환한다")
        void listDefaultStatusActive() {
            // given
            LostPetReport report = LostPetReport.create(
                    10L,
                    "Momo",
                    "Poodle",
                    "https://cdn/momo.jpg",
                    "desc",
                    LocalDateTime.now(),
                    "Gangnam"
            );
            report.assignIdForTest(1L);
            PageRequest pageable = PageRequest.of(0, 10);
            Slice<LostPetReport> slice = new SliceImpl<>(java.util.List.of(report), pageable, false);
            given(lostPetReportQueryRepository.findByOwnerAndStatus(10L, LostPetReportStatus.ACTIVE, pageable))
                    .willReturn(slice);

            // when
            Slice<LostPetSummaryResponse> response = lostPetService.list(10L, null, pageable);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).lostPetId()).isEqualTo(1L);
            assertThat(response.getContent().get(0).status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("실종 신고 상세 조회")
    class DetailLostPet {

        @Test
        @DisplayName("본인 신고는 상세를 조회할 수 있다")
        void detailSuccess() {
            // given
            LostPetReport report = LostPetReport.create(
                    10L,
                    "Momo",
                    "Poodle",
                    "https://cdn/momo.jpg",
                    "desc",
                    LocalDateTime.now(),
                    "Gangnam"
            );
            report.assignIdForTest(1L);
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));

            // when
            LostPetDetailResponse response = lostPetService.detail(10L, 1L);

            // then
            assertThat(response.lostPetId()).isEqualTo(1L);
            assertThat(response.ownerId()).isEqualTo(10L);
            assertThat(response.petName()).isEqualTo("Momo");
        }

        @Test
        @DisplayName("타인 신고를 조회하면 권한 예외가 발생한다")
        void detailForbidden() {
            // given
            LostPetReport report = LostPetReport.create(
                    99L,
                    "Momo",
                    "Poodle",
                    "https://cdn/momo.jpg",
                    "desc",
                    LocalDateTime.now(),
                    "Gangnam"
            );
            report.assignIdForTest(1L);
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> lostPetService.detail(10L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", LostPetErrorCode.L403_FORBIDDEN);
        }
    }
}
