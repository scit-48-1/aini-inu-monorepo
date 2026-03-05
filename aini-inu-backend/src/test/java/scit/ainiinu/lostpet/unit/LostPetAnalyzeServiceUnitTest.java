package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClient;
import scit.ainiinu.lostpet.integration.ai.LostPetAiResult;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.repository.SightingRepository;
import scit.ainiinu.lostpet.service.LostPetCandidateScoringService;
import scit.ainiinu.lostpet.service.LostPetAnalyzeService;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class LostPetAnalyzeServiceUnitTest {

    @Mock
    private LostPetAiClient lostPetAiClient;

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @Mock
    private SightingRepository sightingRepository;

    @Mock
    private LostPetSearchSessionRepository lostPetSearchSessionRepository;

    @Mock
    private LostPetSearchCandidateRepository lostPetSearchCandidateRepository;

    @Mock
    private LostPetCandidateScoringService lostPetCandidateScoringService;

    @InjectMocks
    private LostPetAnalyzeService lostPetAnalyzeService;

    @Nested
    @DisplayName("AI 분석")
    class Analyze {

        @Test
        @DisplayName("외부 분석이 성공하면 후보를 반환한다")
        void success() {
            LostPetReport report = LostPetReport.create(
                    1L,
                    "Momo",
                    "Poodle",
                    "https://cdn/momo.jpg",
                    "desc",
                    LocalDateTime.now(),
                    "Gangnam"
            );
            report.assignIdForTest(10L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    1L,
                    report,
                    "LOST",
                    "https://cdn/sample.jpg",
                    null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 101L);

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("ok", List.of()));
            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(10L)
                    .imageUrl("https://cdn/sample.jpg")
                    .mode("LOST")
                    .build();

            LostPetAnalyzeResponse response = lostPetAnalyzeService.analyze(1L, request);

            assertThat(response.summary()).isEqualTo("ok");
            assertThat(response.sessionId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("외부 분석 실패 시 500 도메인 예외를 던지고 세션을 생성하지 않는다")
        void failWithoutSessionCreation() {
            LostPetReport report = LostPetReport.create(
                    1L,
                    "Momo",
                    "Poodle",
                    "https://cdn/momo.jpg",
                    "desc",
                    LocalDateTime.now(),
                    "Gangnam"
            );
            report.assignIdForTest(10L);

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetAiClient.analyze(any())).willThrow(new RuntimeException("timeout"));
            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(10L)
                    .imageUrl("https://cdn/sample.jpg")
                    .mode("LOST")
                    .build();

            assertThatThrownBy(() -> lostPetAnalyzeService.analyze(1L, request))
                    .isInstanceOf(LostPetException.class)
                    .hasFieldOrPropertyWithValue("errorCode", LostPetErrorCode.L500_AI_ANALYZE_FAILED);
            then(lostPetSearchSessionRepository).should(never()).save(any(LostPetSearchSession.class));
            then(lostPetSearchCandidateRepository).should(never()).saveAll(any());
        }
    }
}
