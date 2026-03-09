package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LostPetMatchQueryServiceUnitTest {

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @Mock
    private LostPetSearchSessionRepository lostPetSearchSessionRepository;

    @Mock
    private LostPetSearchCandidateRepository lostPetSearchCandidateRepository;

    @Mock
    private MemberRepository memberRepository;

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
        @DisplayName("세션 미지정 시 최신 유효 세션 후보를 반환한다 - 제보 상세 정보 포함")
        void successWithLatestSession() {
            // given
            Long finderId = 22L;
            LocalDateTime foundAt = LocalDateTime.of(2025, 6, 15, 14, 30);
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);
            Sighting sighting = Sighting.create(
                    finderId, "https://cdn/found-dog.jpg", foundAt, "서울시 강남구 역삼동", "갈색 푸들 발견"
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

            Member finder = Member.builder()
                    .email("finder@test.com")
                    .nickname("제보자닉네임")
                    .memberType(MemberType.NON_PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(finder, "id", finderId);

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findTopByOwnerIdAndLostPetReportIdOrderByCreatedAtDesc(10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(100L, PageRequest.of(0, 20)))
                    .willReturn(new SliceImpl<>(List.of(candidate), PageRequest.of(0, 20), false));
            given(memberRepository.findAllById(List.of(finderId)))
                    .willReturn(List.of(finder));

            // when
            List<LostPetMatchCandidateResponse> content = lostPetMatchQueryService.findCandidates(
                    1L,
                    10L,
                    null,
                    PageRequest.of(0, 20)
            ).getContent();

            // then
            assertThat(content).hasSize(1);
            LostPetMatchCandidateResponse response = content.get(0);
            assertThat(response.sessionId()).isEqualTo(100L);
            assertThat(response.scoreTotal()).isEqualByComparingTo("0.85000");
            assertThat(response.photoUrl()).isEqualTo("https://cdn/found-dog.jpg");
            assertThat(response.foundLocation()).isEqualTo("서울시 강남구 역삼동");
            assertThat(response.foundAt()).isEqualTo(foundAt);
            assertThat(response.memo()).isEqualTo("갈색 푸들 발견");
            assertThat(response.finderNickname()).isEqualTo("제보자닉네임");
        }

        @Test
        @DisplayName("제보자 회원 정보가 없으면 닉네임을 '알 수 없음'으로 반환한다")
        void fallbackNicknameWhenMemberNotFound() {
            // given
            Long finderId = 99L;
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);
            Sighting sighting = Sighting.create(
                    finderId, "https://cdn/photo.jpg", LocalDateTime.now(), "역삼동", null
            );
            sighting.assignIdForTest(3L);
            LostPetSearchSession session = LostPetSearchSession.create(
                    10L, report, "LOST", "u", null, LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 100L);
            LostPetSearchCandidate candidate = LostPetSearchCandidate.create(
                    session, sighting,
                    new BigDecimal("0.80000"), new BigDecimal("0.50000"),
                    new BigDecimal("0.60000"), new BigDecimal("0.70000"), 1
            );

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findTopByOwnerIdAndLostPetReportIdOrderByCreatedAtDesc(10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(100L, PageRequest.of(0, 20)))
                    .willReturn(new SliceImpl<>(List.of(candidate), PageRequest.of(0, 20), false));
            given(memberRepository.findAllById(List.of(finderId)))
                    .willReturn(List.of());

            // when
            List<LostPetMatchCandidateResponse> content = lostPetMatchQueryService.findCandidates(
                    1L, 10L, null, PageRequest.of(0, 20)
            ).getContent();

            // then
            assertThat(content).hasSize(1);
            assertThat(content.get(0).finderNickname()).isEqualTo("알 수 없음");
            assertThat(content.get(0).memo()).isNull();
        }

        @Test
        @DisplayName("후보가 여러 명일 때 각각의 제보자 닉네임을 올바르게 매핑한다")
        void multipleCandidatesWithDifferentFinders() {
            // given
            Long finder1Id = 11L;
            Long finder2Id = 22L;
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);

            Sighting sighting1 = Sighting.create(finder1Id, "https://cdn/p1.jpg", LocalDateTime.now(), "역삼동", "메모1");
            sighting1.assignIdForTest(2L);
            Sighting sighting2 = Sighting.create(finder2Id, "https://cdn/p2.jpg", LocalDateTime.now(), "삼성동", null);
            sighting2.assignIdForTest(3L);

            LostPetSearchSession session = LostPetSearchSession.create(
                    10L, report, "LOST", "u", null, LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 100L);

            LostPetSearchCandidate candidate1 = LostPetSearchCandidate.create(
                    session, sighting1,
                    new BigDecimal("0.90000"), new BigDecimal("0.60000"),
                    new BigDecimal("0.70000"), new BigDecimal("0.85000"), 1
            );
            LostPetSearchCandidate candidate2 = LostPetSearchCandidate.create(
                    session, sighting2,
                    new BigDecimal("0.80000"), new BigDecimal("0.50000"),
                    new BigDecimal("0.60000"), new BigDecimal("0.70000"), 2
            );

            Member member1 = Member.builder().email("a@test.com").nickname("제보자A").memberType(MemberType.NON_PET_OWNER).build();
            ReflectionTestUtils.setField(member1, "id", finder1Id);
            Member member2 = Member.builder().email("b@test.com").nickname("제보자B").memberType(MemberType.NON_PET_OWNER).build();
            ReflectionTestUtils.setField(member2, "id", finder2Id);

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findTopByOwnerIdAndLostPetReportIdOrderByCreatedAtDesc(10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdOrderByRankOrderAsc(100L, PageRequest.of(0, 20)))
                    .willReturn(new SliceImpl<>(List.of(candidate1, candidate2), PageRequest.of(0, 20), false));
            given(memberRepository.findAllById(List.of(finder1Id, finder2Id)))
                    .willReturn(List.of(member1, member2));

            // when
            List<LostPetMatchCandidateResponse> content = lostPetMatchQueryService.findCandidates(
                    1L, 10L, null, PageRequest.of(0, 20)
            ).getContent();

            // then
            assertThat(content).hasSize(2);
            assertThat(content.get(0).finderNickname()).isEqualTo("제보자A");
            assertThat(content.get(0).foundLocation()).isEqualTo("역삼동");
            assertThat(content.get(0).memo()).isEqualTo("메모1");
            assertThat(content.get(1).finderNickname()).isEqualTo("제보자B");
            assertThat(content.get(1).foundLocation()).isEqualTo("삼성동");
            assertThat(content.get(1).memo()).isNull();
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
