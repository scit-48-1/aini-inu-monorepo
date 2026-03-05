package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetMatchCandidateResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.service.LostPetMatchQueryService;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LostPetMatchQueryServiceUnitTest {

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @Mock
    private LostPetSearchSessionRepository lostPetSearchSessionRepository;

    @Mock
    private LostPetSearchCandidateRepository lostPetSearchCandidateRepository;

    @InjectMocks
    private LostPetMatchQueryService lostPetMatchQueryService;

    @Nested
    @DisplayName("세션 기반 후보 조회")
    class FindCandidates {

        @Test
        @DisplayName("세션이 만료되면 예외를 반환한다")
        void failWhenExpiredSession() {
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    10L,
                    report,
                    "LOST",
                    "u",
                    null,
                    LocalDateTime.now().minusMinutes(1)
            );
            ReflectionTestUtils.setField(session, "id", 100L);

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));

            assertThatThrownBy(() -> lostPetMatchQueryService.findCandidates(
                    1L,
                    10L,
                    100L,
                    PageRequest.of(0, 20)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode().getCode())
                                .isEqualTo(LostPetErrorCode.L410_SEARCH_SESSION_EXPIRED.getCode());
                    });
        }

        @Test
        @DisplayName("세션 미지정 시 최신 유효 세션 후보를 반환한다")
        void successWithLatestSession() {
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);
            Sighting sighting = Sighting.create(
                    22L, "u2", LocalDateTime.now(), "Yeoksam", "m"
            );
            sighting.assignIdForTest(2L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    10L,
                    report,
                    "LOST",
                    "u",
                    null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 100L);
            LostPetSearchCandidate candidate = LostPetSearchCandidate.create(
                    session,
                    sighting,
                    new BigDecimal("0.90000"),
                    new BigDecimal("0.60000"),
                    new BigDecimal("0.70000"),
                    new BigDecimal("0.85000"),
                    1
            );

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findTopByOwnerIdAndLostPetReportIdOrderByCreatedAtDesc(10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(100L, PageRequest.of(0, 20)))
                    .willReturn(new SliceImpl<>(List.of(candidate), PageRequest.of(0, 20), false));

            List<LostPetMatchCandidateResponse> content = lostPetMatchQueryService.findCandidates(
                    1L,
                    10L,
                    null,
                    PageRequest.of(0, 20)
            ).getContent();

            assertThat(content).hasSize(1);
            assertThat(content.get(0).sessionId()).isEqualTo(100L);
            assertThat(content.get(0).scoreTotal()).isEqualByComparingTo("0.85000");
        }

        @Test
        @DisplayName("요청자가 견주가 아니면 권한 예외를 반환한다")
        void failWhenNotOwner() {
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));

            assertThatThrownBy(() -> lostPetMatchQueryService.findCandidates(
                    1L,
                    99L,
                    null,
                    PageRequest.of(0, 20)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode().getCode())
                                .isEqualTo(LostPetErrorCode.L403_FORBIDDEN.getCode());
                    });
        }
    }
}
