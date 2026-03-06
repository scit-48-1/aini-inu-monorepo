package scit.ainiinu.lostpet.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetMatchInvalidatedReason;
import scit.ainiinu.lostpet.domain.LostPetMatchStatus;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.repository.LostPetMatchRepository;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.service.LostPetResolutionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LostPetResolutionServiceUnitTest {

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @Mock
    private LostPetMatchRepository lostPetMatchRepository;

    @InjectMocks
    private LostPetResolutionService lostPetResolutionService;

    @Nested
    @DisplayName("resolveReport")
    class ResolveReport {

        @Test
        @DisplayName("정상적으로 신고를 해결하고 활성 매치를 무효화한다")
        void success_resolvesAndInvalidatesMatches() {
            // given
            Long memberId = 1L;
            Long lostPetId = 10L;

            LostPetReport report = LostPetReport.create(
                    memberId, "멍멍이", "푸들", "photo.jpg",
                    "잃어버렸어요", LocalDateTime.now(), "서울숲"
            );
            report.assignIdForTest(lostPetId);

            LostPetMatch match = createMatch(report, LostPetMatchStatus.PENDING_APPROVAL);

            given(lostPetReportRepository.findById(lostPetId)).willReturn(Optional.of(report));
            given(lostPetMatchRepository.findByLostPetReportIdAndStatusIn(
                    eq(lostPetId),
                    eq(List.of(LostPetMatchStatus.PENDING_APPROVAL, LostPetMatchStatus.APPROVED, LostPetMatchStatus.PENDING_CHAT_LINK))
            )).willReturn(List.of(match));

            // when
            lostPetResolutionService.resolveReport(memberId, lostPetId);

            // then
            assertThat(report.getStatus()).isEqualTo(LostPetReportStatus.RESOLVED);
            assertThat(match.getStatus()).isEqualTo(LostPetMatchStatus.INVALIDATED);
            assertThat(match.getInvalidatedReason()).isEqualTo(LostPetMatchInvalidatedReason.REPORT_RESOLVED);
        }

        @Test
        @DisplayName("존재하지 않는 신고이면 L404_NOT_FOUND 예외")
        void notFound_throwsException() {
            // given
            Long memberId = 1L;
            Long lostPetId = 999L;

            given(lostPetReportRepository.findById(lostPetId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> lostPetResolutionService.resolveReport(memberId, lostPetId))
                    .isInstanceOf(LostPetException.class)
                    .hasFieldOrPropertyWithValue("errorCode", LostPetErrorCode.L404_NOT_FOUND);
        }

        @Test
        @DisplayName("신고 작성자가 아니면 L403_FORBIDDEN 예외")
        void notOwner_throwsException() {
            // given
            Long ownerId = 1L;
            Long otherMemberId = 99L;
            Long lostPetId = 10L;

            LostPetReport report = LostPetReport.create(
                    ownerId, "멍멍이", "푸들", "photo.jpg",
                    "잃어버렸어요", LocalDateTime.now(), "서울숲"
            );
            report.assignIdForTest(lostPetId);

            given(lostPetReportRepository.findById(lostPetId)).willReturn(Optional.of(report));

            // when & then
            assertThatThrownBy(() -> lostPetResolutionService.resolveReport(otherMemberId, lostPetId))
                    .isInstanceOf(LostPetException.class)
                    .hasFieldOrPropertyWithValue("errorCode", LostPetErrorCode.L403_FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("isResolved")
    class IsResolved {

        @Test
        @DisplayName("해결된 신고이면 true 반환")
        void resolved_returnsTrue() {
            // given
            Long lostPetId = 10L;
            LostPetReport report = LostPetReport.create(
                    1L, "멍멍이", "푸들", "photo.jpg",
                    "잃어버렸어요", LocalDateTime.now(), "서울숲"
            );
            report.resolve();

            given(lostPetReportRepository.findById(lostPetId)).willReturn(Optional.of(report));

            // when
            boolean result = lostPetResolutionService.isResolved(lostPetId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("활성 신고이면 false 반환")
        void active_returnsFalse() {
            // given
            Long lostPetId = 10L;
            LostPetReport report = LostPetReport.create(
                    1L, "멍멍이", "푸들", "photo.jpg",
                    "잃어버렸어요", LocalDateTime.now(), "서울숲"
            );

            given(lostPetReportRepository.findById(lostPetId)).willReturn(Optional.of(report));

            // when
            boolean result = lostPetResolutionService.isResolved(lostPetId);

            // then
            assertThat(result).isFalse();
        }
    }

    private LostPetMatch createMatch(LostPetReport report, LostPetMatchStatus initialStatus) {
        // Use Sighting mock via reflection since we can't easily construct it
        LostPetMatch match = LostPetMatch.create(report, null, BigDecimal.valueOf(0.85));
        if (initialStatus == LostPetMatchStatus.APPROVED) {
            match.approve(1L);
        }
        return match;
    }
}
