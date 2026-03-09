package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
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
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeCandidateResponse;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.error.LostPetException;
import scit.ainiinu.lostpet.integration.ai.LostPetAiCandidate;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClient;
import scit.ainiinu.lostpet.integration.ai.LostPetAiResult;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.repository.SightingRepository;
import scit.ainiinu.lostpet.service.LostPetCandidateScoringService;
import scit.ainiinu.lostpet.service.LostPetAnalyzeService;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;

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

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private LostPetAnalyzeService lostPetAnalyzeService;

    @Nested
    @DisplayName("AI 분석")
    class Analyze {

        @Test
        @DisplayName("외부 분석이 성공하고 후보가 없으면 빈 리스트를 반환한다")
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
            assertThat(response.candidates()).isEmpty();
        }

        @Test
        @DisplayName("AI 후보가 있을 때 제보 상세 정보와 제보자 닉네임이 포함된다")
        void successWithCandidatesIncludesSightingDetails() {
            // given
            ReflectionTestUtils.setField(lostPetAnalyzeService, "topN", 20);
            ReflectionTestUtils.setField(lostPetAnalyzeService, "sessionTtlHours", 24L);
            Long finderId = 33L;
            LocalDateTime foundAt = LocalDateTime.of(2025, 7, 1, 10, 0);
            LostPetReport report = LostPetReport.create(
                    1L, "초코", "말티즈", "https://cdn/choco.jpg", "갈색 소형견",
                    LocalDateTime.now(), "서울시 강남구"
            );
            report.assignIdForTest(10L);

            Sighting sighting = Sighting.create(
                    finderId, "https://cdn/found.jpg", foundAt, "서울시 서초구 반포동", "골목에서 발견"
            );
            sighting.assignIdForTest(5L);

            LostPetSearchSession session = LostPetSearchSession.create(
                    1L, report, "LOST", "https://cdn/choco.jpg", null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 101L);

            LostPetAiCandidate aiCandidate = new LostPetAiCandidate(5L, finderId, new BigDecimal("0.85"));
            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("발견", List.of(aiCandidate)));
            given(sightingRepository.findById(5L)).willReturn(Optional.of(sighting));
            given(lostPetCandidateScoringService.normalizeSimilarity(any()))
                    .willReturn(new BigDecimal("4.25000"));
            given(lostPetCandidateScoringService.computeDistanceScore(any(), any()))
                    .willReturn(new BigDecimal("3.00000"));
            given(lostPetCandidateScoringService.computeRecencyScore(any(), any()))
                    .willReturn(new BigDecimal("2.50000"));
            given(lostPetCandidateScoringService.computeTotalScore(any(), any(), any()))
                    .willReturn(new BigDecimal("9.75000"));

            LostPetSearchCandidate savedCandidate = LostPetSearchCandidate.create(
                    session, sighting,
                    new BigDecimal("4.25000"), new BigDecimal("3.00000"),
                    new BigDecimal("2.50000"), new BigDecimal("9.75000"), 1
            );
            given(lostPetSearchCandidateRepository.saveAll(any()))
                    .willReturn(List.of(savedCandidate));

            Member finder = Member.builder()
                    .email("finder@test.com")
                    .nickname("동물사랑")
                    .memberType(MemberType.NON_PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(finder, "id", finderId);
            given(memberRepository.findAllById(List.of(finderId)))
                    .willReturn(List.of(finder));

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(10L)
                    .imageUrl("https://cdn/choco.jpg")
                    .mode("LOST")
                    .build();

            // when
            LostPetAnalyzeResponse response = lostPetAnalyzeService.analyze(1L, request);

            // then
            assertThat(response.candidates()).hasSize(1);
            LostPetAnalyzeCandidateResponse candidate = response.candidates().get(0);
            assertThat(candidate.sightingId()).isEqualTo(5L);
            assertThat(candidate.photoUrl()).isEqualTo("https://cdn/found.jpg");
            assertThat(candidate.foundLocation()).isEqualTo("서울시 서초구 반포동");
            assertThat(candidate.foundAt()).isEqualTo(foundAt);
            assertThat(candidate.memo()).isEqualTo("골목에서 발견");
            assertThat(candidate.finderNickname()).isEqualTo("동물사랑");
            assertThat(candidate.scoreTotal()).isEqualByComparingTo("9.75000");
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
    @DisplayName("본인 제보 필터링")
    class SelfSightingFilter {

        @Test
        @DisplayName("신고자와 제보자가 동일하면 후보에서 제외된다")
        void excludesSelfSighting() {
            // given
            ReflectionTestUtils.setField(lostPetAnalyzeService, "topN", 20);
            ReflectionTestUtils.setField(lostPetAnalyzeService, "sessionTtlHours", 24L);

            Long ownerId = 1L;
            LostPetReport report = LostPetReport.create(
                    ownerId, "Momo", "Poodle", "https://cdn/momo.jpg", "desc",
                    LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(10L);

            // 제보자(finderId) == 신고자(ownerId)인 제보
            Sighting selfSighting = Sighting.create(
                    ownerId, "https://cdn/self.jpg", LocalDateTime.now(), "Gangnam", "내가 발견"
            );
            selfSighting.assignIdForTest(5L);

            LostPetSearchSession session = LostPetSearchSession.create(
                    ownerId, report, "LOST", "https://cdn/momo.jpg", null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 101L);

            LostPetAiCandidate aiCandidate = new LostPetAiCandidate(5L, ownerId, new BigDecimal("0.95"));

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("결과", List.of(aiCandidate)));
            given(sightingRepository.findById(5L)).willReturn(Optional.of(selfSighting));

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(10L)
                    .imageUrl("https://cdn/momo.jpg")
                    .mode("LOST")
                    .build();

            // when
            LostPetAnalyzeResponse response = lostPetAnalyzeService.analyze(ownerId, request);

            // then - 본인 제보는 필터링되어 후보가 비어있어야 함
            assertThat(response.candidates()).isEmpty();
            then(lostPetSearchCandidateRepository).should(never()).saveAll(any());
        }

        @Test
        @DisplayName("본인 제보가 섞여 있으면 타인의 제보만 후보로 남는다")
        void keepOnlyOtherSightings() {
            // given
            ReflectionTestUtils.setField(lostPetAnalyzeService, "topN", 20);
            ReflectionTestUtils.setField(lostPetAnalyzeService, "sessionTtlHours", 24L);

            Long ownerId = 1L;
            Long otherFinderId = 33L;
            LostPetReport report = LostPetReport.create(
                    ownerId, "초코", "말티즈", "https://cdn/choco.jpg", "갈색 소형견",
                    LocalDateTime.now(), "서울시 강남구"
            );
            report.assignIdForTest(10L);

            // 본인 제보 (필터링 대상)
            Sighting selfSighting = Sighting.create(
                    ownerId, "https://cdn/self.jpg", LocalDateTime.now(), "Gangnam", "내가 발견"
            );
            selfSighting.assignIdForTest(5L);

            // 타인 제보 (유지 대상)
            Sighting otherSighting = Sighting.create(
                    otherFinderId, "https://cdn/other.jpg", LocalDateTime.now(), "Yeoksam", "다른사람 발견"
            );
            otherSighting.assignIdForTest(6L);

            LostPetSearchSession session = LostPetSearchSession.create(
                    ownerId, report, "LOST", "https://cdn/choco.jpg", null,
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 101L);

            LostPetAiCandidate selfAiCandidate = new LostPetAiCandidate(5L, ownerId, new BigDecimal("0.95"));
            LostPetAiCandidate otherAiCandidate = new LostPetAiCandidate(6L, otherFinderId, new BigDecimal("0.80"));

            given(lostPetReportRepository.findById(anyLong())).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.save(any(LostPetSearchSession.class))).willReturn(session);
            given(lostPetAiClient.analyze(any()))
                    .willReturn(new LostPetAiResult("결과", List.of(selfAiCandidate, otherAiCandidate)));
            given(sightingRepository.findById(5L)).willReturn(Optional.of(selfSighting));
            given(sightingRepository.findById(6L)).willReturn(Optional.of(otherSighting));
            given(lostPetCandidateScoringService.normalizeSimilarity(any()))
                    .willReturn(new BigDecimal("4.00000"));
            given(lostPetCandidateScoringService.computeDistanceScore(any(), any()))
                    .willReturn(new BigDecimal("3.00000"));
            given(lostPetCandidateScoringService.computeRecencyScore(any(), any()))
                    .willReturn(new BigDecimal("2.50000"));
            given(lostPetCandidateScoringService.computeTotalScore(any(), any(), any()))
                    .willReturn(new BigDecimal("9.50000"));

            LostPetSearchCandidate savedCandidate = LostPetSearchCandidate.create(
                    session, otherSighting,
                    new BigDecimal("4.00000"), new BigDecimal("3.00000"),
                    new BigDecimal("2.50000"), new BigDecimal("9.50000"), 1
            );
            given(lostPetSearchCandidateRepository.saveAll(any()))
                    .willReturn(List.of(savedCandidate));

            Member finder = Member.builder()
                    .email("finder@test.com")
                    .nickname("제보자")
                    .memberType(MemberType.NON_PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(finder, "id", otherFinderId);
            given(memberRepository.findAllById(List.of(otherFinderId)))
                    .willReturn(List.of(finder));

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(10L)
                    .imageUrl("https://cdn/choco.jpg")
                    .mode("LOST")
                    .build();

            // when
            LostPetAnalyzeResponse response = lostPetAnalyzeService.analyze(ownerId, request);

            // then - 본인 제보(sightingId=5)는 제외, 타인 제보(sightingId=6)만 포함
            assertThat(response.candidates()).hasSize(1);
            assertThat(response.candidates().get(0).sightingId()).isEqualTo(6L);
            assertThat(response.candidates().get(0).finderNickname()).isEqualTo("제보자");
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
