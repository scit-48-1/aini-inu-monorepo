package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Nested
    @DisplayName("쿼리 텍스트 보강")
    class EnrichQueryText {

        @Test
        @DisplayName("실종 신고의 petName, breed, description, lastSeenLocation이 queryText에 포함된다")
        void enrichesQueryTextFromReport() {
            // given
            LostPetReport report = LostPetReport.create(
                    1L,
                    "초코",
                    "말티즈",
                    "https://cdn/choco.jpg",
                    "갈색 곱슬 소형견 빨간 목줄",
                    LocalDateTime.now(),
                    "서울시 강남구 역삼동"
            );
            report.assignIdForTest(10L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    1L, report, "LOST", "https://cdn/choco.jpg", null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 200L);

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("ok", List.of()));

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(10L)
                    .imageUrl("https://cdn/choco.jpg")
                    .build();

            // when
            lostPetAnalyzeService.analyze(1L, request);

            // then
            ArgumentCaptor<LostPetAnalyzeRequest> captor = ArgumentCaptor.forClass(LostPetAnalyzeRequest.class);
            verify(lostPetAiClient).analyze(captor.capture());
            String enrichedQueryText = captor.getValue().getQueryText();
            assertThat(enrichedQueryText).contains("초코");
            assertThat(enrichedQueryText).contains("말티즈");
            assertThat(enrichedQueryText).contains("갈색 곱슬 소형견 빨간 목줄");
            assertThat(enrichedQueryText).contains("서울시 강남구 역삼동");
        }

        @Test
        @DisplayName("사용자가 직접 입력한 queryText가 보강 텍스트 뒤에 보존된다")
        void preservesOriginalQueryText() {
            // given
            LostPetReport report = LostPetReport.create(
                    1L,
                    "뭉치",
                    "포메라니안",
                    "https://cdn/mung.jpg",
                    "하얀색 소형견",
                    LocalDateTime.now(),
                    "부산시 해운대구"
            );
            report.assignIdForTest(11L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    1L, report, "LOST", "https://cdn/mung.jpg", null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 201L);

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("ok", List.of()));

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(11L)
                    .imageUrl("https://cdn/mung.jpg")
                    .queryText("꼬리가 짧아요")
                    .build();

            // when
            lostPetAnalyzeService.analyze(1L, request);

            // then
            ArgumentCaptor<LostPetAnalyzeRequest> captor = ArgumentCaptor.forClass(LostPetAnalyzeRequest.class);
            verify(lostPetAiClient).analyze(captor.capture());
            String enrichedQueryText = captor.getValue().getQueryText();
            assertThat(enrichedQueryText).contains("뭉치");
            assertThat(enrichedQueryText).contains("포메라니안");
            assertThat(enrichedQueryText).contains("꼬리가 짧아요");
        }

        @Test
        @DisplayName("신고 정보에 null/blank 필드가 있으면 해당 필드는 건너뛴다")
        void skipsNullAndBlankFields() {
            // given
            LostPetReport report = LostPetReport.create(
                    1L,
                    "바둑이",
                    null,           // breed null
                    "https://cdn/baduk.jpg",
                    "   ",          // description blank
                    LocalDateTime.now(),
                    "대전시 서구"
            );
            report.assignIdForTest(12L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    1L, report, "LOST", "https://cdn/baduk.jpg", null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 202L);

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("ok", List.of()));

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(12L)
                    .imageUrl("https://cdn/baduk.jpg")
                    .build();

            // when
            lostPetAnalyzeService.analyze(1L, request);

            // then
            ArgumentCaptor<LostPetAnalyzeRequest> captor = ArgumentCaptor.forClass(LostPetAnalyzeRequest.class);
            verify(lostPetAiClient).analyze(captor.capture());
            String enrichedQueryText = captor.getValue().getQueryText();
            assertThat(enrichedQueryText).contains("바둑이");
            assertThat(enrichedQueryText).contains("대전시 서구");
            assertThat(enrichedQueryText).doesNotContain("null");
        }
    }
}
