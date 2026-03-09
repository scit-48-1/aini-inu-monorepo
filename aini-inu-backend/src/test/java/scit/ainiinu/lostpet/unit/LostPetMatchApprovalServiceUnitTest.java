package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetMatchStatus;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetMatchApproveRequest;
import scit.ainiinu.lostpet.dto.LostPetMatchResponse;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.integration.chat.ChatRoomDirectClient;
import scit.ainiinu.lostpet.repository.LostPetMatchRepository;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.repository.SightingRepository;
import scit.ainiinu.lostpet.service.LostPetMatchApprovalService;

@ExtendWith(MockitoExtension.class)
class LostPetMatchApprovalServiceUnitTest {

    @Mock
    private LostPetReportRepository lostPetReportRepository;

    @Mock
    private SightingRepository sightingRepository;

    @Mock
    private LostPetMatchRepository lostPetMatchRepository;

    @Mock
    private LostPetSearchSessionRepository lostPetSearchSessionRepository;

    @Mock
    private LostPetSearchCandidateRepository lostPetSearchCandidateRepository;

    @Mock
    private ChatRoomDirectClient chatRoomDirectClient;

    @InjectMocks
    private LostPetMatchApprovalService lostPetMatchApprovalService;

    // -- 테스트 픽스처 헬퍼 --

    private LostPetReport createReport(Long ownerId, Long reportId) {
        LostPetReport report = LostPetReport.create(
                ownerId, "Momo", "Poodle", "u", "d", LocalDateTime.now(), "Gangnam"
        );
        report.assignIdForTest(reportId);
        return report;
    }

    private Sighting createSighting(Long finderId, Long sightingId) {
        Sighting sighting = Sighting.create(
                finderId, "u2", LocalDateTime.now(), "Yeoksam", "m"
        );
        sighting.assignIdForTest(sightingId);
        return sighting;
    }

    private LostPetSearchSession createSession(Long ownerId, LostPetReport report, Long sessionId, LocalDateTime expiresAt) {
        LostPetSearchSession session = LostPetSearchSession.create(
                ownerId, report, "LOST", "u", null, expiresAt
        );
        ReflectionTestUtils.setField(session, "id", sessionId);
        return session;
    }

    private LostPetSearchCandidate createCandidate(LostPetSearchSession session, Sighting sighting) {
        return LostPetSearchCandidate.create(
                session, sighting,
                new BigDecimal("0.90000"),
                new BigDecimal("0.50000"),
                new BigDecimal("0.70000"),
                new BigDecimal("0.81000"),
                1
        );
    }

    private void stubHappyPath(LostPetReport report, LostPetSearchSession session,
                               LostPetSearchCandidate candidate, Sighting sighting) {
        given(lostPetReportRepository.findById(report.getId())).willReturn(Optional.of(report));
        given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(
                session.getId(), report.getOwnerId(), report.getId()))
                .willReturn(Optional.of(session));
        given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(session.getId(), sighting.getId()))
                .willReturn(Optional.of(candidate));
        given(sightingRepository.findById(sighting.getId())).willReturn(Optional.of(sighting));
    }

    @Nested
    @DisplayName("매치 승인 - 성공 케이스")
    class ApproveSuccess {

        @Test
        @DisplayName("채팅 생성 성공 시 CHAT_LINKED 상태를 반환한다")
        void chatLinked() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(eq(10L), eq(22L), any(), any())).willReturn(555L);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            );

            assertThat(response.status()).isEqualTo("CHAT_LINKED");
            assertThat(response.chatRoomId()).isEqualTo(555L);
        }

        @Test
        @DisplayName("채팅방 생성 시 memberId와 finderId를 올바르게 전달한다")
        void passesCorrectMemberAndPartnerIds() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(eq(10L), eq(22L), eq("LOST_PET"), any())).willReturn(100L);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            lostPetMatchApprovalService.approve(1L, 10L, new LostPetMatchApproveRequest(100L, 2L));

            verify(chatRoomDirectClient).createDirectRoom(eq(10L), eq(22L), eq("LOST_PET"), any());
        }

        @Test
        @DisplayName("breed가 있으면 roomTitle에 breed + petName을 포함한다")
        void roomTitleIncludesBreedAndPetName() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(any(), any(), any(), any())).willReturn(100L);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            lostPetMatchApprovalService.approve(1L, 10L, new LostPetMatchApproveRequest(100L, 2L));

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(chatRoomDirectClient).createDirectRoom(any(), any(), any(), titleCaptor.capture());
            assertThat(titleCaptor.getValue()).isEqualTo("Poodle Momo를 찾습니다");
        }

        @Test
        @DisplayName("breed가 null이면 roomTitle에 petName만 포함한다")
        void roomTitleWithoutBreed() {
            LostPetReport report = LostPetReport.create(
                    10L, "Momo", null, "u", "d", LocalDateTime.now(), "Gangnam"
            );
            report.assignIdForTest(1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(any(), any(), any(), any())).willReturn(100L);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            lostPetMatchApprovalService.approve(1L, 10L, new LostPetMatchApproveRequest(100L, 2L));

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(chatRoomDirectClient).createDirectRoom(any(), any(), any(), titleCaptor.capture());
            assertThat(titleCaptor.getValue()).isEqualTo("Momo를 찾습니다");
        }

        @Test
        @DisplayName("이미 CHAT_LINKED 상태인 매치는 채팅 생성을 건너뛰고 기존 응답을 반환한다")
        void idempotentWhenAlreadyChatLinked() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            LostPetMatch existingMatch = LostPetMatch.create(report, sighting, candidate.getScoreTotal());
            existingMatch.approve(10L);
            existingMatch.linkChatRoom(999L);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.of(existingMatch));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            );

            assertThat(response.status()).isEqualTo("CHAT_LINKED");
            assertThat(response.chatRoomId()).isEqualTo(999L);
            verify(chatRoomDirectClient, never()).createDirectRoom(any(), any(), any(), any());
            verify(lostPetMatchRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("매치 승인 - 채팅 생성 실패 케이스")
    class ApproveChatFailure {

        @Test
        @DisplayName("채팅 생성 실패 시 PENDING_CHAT_LINK 상태를 반환한다")
        void pendingOnChatFailure() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(eq(10L), eq(22L), any(), any()))
                    .willThrow(new RuntimeException("chat down"));
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            );

            assertThat(response.status()).isEqualTo("PENDING_CHAT_LINK");
            assertThat(response.chatRoomId()).isNull();
        }

        @Test
        @DisplayName("채팅 인증 실패 시 PENDING_CHAT_LINK 상태를 반환한다")
        void pendingOnChatAuthFailure() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(eq(10L), eq(22L), any(), any()))
                    .willThrow(new RuntimeException("unauthorized"));
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            );

            assertThat(response.status()).isEqualTo("PENDING_CHAT_LINK");
            assertThat(response.chatRoomId()).isNull();
        }

        @Test
        @DisplayName("채팅 응답 스키마 오류(null roomId) 시 PENDING_CHAT_LINK 상태를 반환한다")
        void pendingOnChatSchemaFailure() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(eq(10L), eq(22L), any(), any())).willReturn(null);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            );

            assertThat(response.status()).isEqualTo("PENDING_CHAT_LINK");
            assertThat(response.chatRoomId()).isNull();
        }

        @Test
        @DisplayName("채팅 생성 실패해도 매치는 DB에 저장된다")
        void matchSavedEvenOnChatFailure() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(eq(10L), eq(22L), any(), any()))
                    .willThrow(new RuntimeException("chat down"));
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            lostPetMatchApprovalService.approve(1L, 10L, new LostPetMatchApproveRequest(100L, 2L));

            ArgumentCaptor<LostPetMatch> matchCaptor = ArgumentCaptor.forClass(LostPetMatch.class);
            verify(lostPetMatchRepository).save(matchCaptor.capture());
            LostPetMatch savedMatch = matchCaptor.getValue();
            assertThat(savedMatch.getStatus()).isEqualTo(LostPetMatchStatus.PENDING_CHAT_LINK);
            assertThat(savedMatch.getChatRoomId()).isNull();
        }
    }

    @Nested
    @DisplayName("매치 승인 - 검증 실패 케이스")
    class ApproveValidationFailure {

        @Test
        @DisplayName("신고를 찾을 수 없으면 L404_NOT_FOUND 예외를 반환한다")
        void failWhenReportNotFound() {
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L404_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("타인의 신고에 접근하면 L403_FORBIDDEN 예외를 반환한다")
        void failWhenNotOwner() {
            LostPetReport report = createReport(10L, 1L);
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 99L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L403_FORBIDDEN.getCode()));
        }

        @Test
        @DisplayName("이미 해결된 신고이면 L410_REPORT_RESOLVED 예외를 반환한다")
        void failWhenReportResolved() {
            LostPetReport report = createReport(10L, 1L);
            ReflectionTestUtils.setField(report, "status",
                    scit.ainiinu.lostpet.domain.LostPetReportStatus.RESOLVED);
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L410_REPORT_RESOLVED.getCode()));
        }

        @Test
        @DisplayName("세션을 찾을 수 없으면 L404_SEARCH_SESSION_NOT_FOUND 예외를 반환한다")
        void failWhenSessionNotFound() {
            LostPetReport report = createReport(10L, 1L);
            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L404_SEARCH_SESSION_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("세션이 만료되면 L410_SEARCH_SESSION_EXPIRED 예외를 반환한다")
        void failWhenSessionExpired() {
            LostPetReport report = createReport(10L, 1L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().minusMinutes(1));

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L410_SEARCH_SESSION_EXPIRED.getCode()));
        }

        @Test
        @DisplayName("세션 후보가 아니면 L409_SEARCH_CANDIDATE_INVALID 예외를 반환한다")
        void failWhenCandidateNotInSession() {
            LostPetReport report = createReport(10L, 1L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(100L, 2L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L409_SEARCH_CANDIDATE_INVALID.getCode()));
        }

        @Test
        @DisplayName("목격 정보가 CLOSED이면 L409_MATCH_CONFLICT 예외를 반환한다")
        void failWhenSightingClosed() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            sighting.close();
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L409_MATCH_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("매치가 INVALIDATED 상태이면 L409_MATCH_CONFLICT 예외를 반환한다")
        void failWhenMatchInvalidated() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            LostPetMatch invalidatedMatch = LostPetMatch.create(report, sighting, candidate.getScoreTotal());
            invalidatedMatch.invalidate(scit.ainiinu.lostpet.domain.LostPetMatchInvalidatedReason.SIGHTING_CLOSED);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.of(invalidatedMatch));

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L409_MATCH_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("본인의 제보를 승인하면 L409_SELF_MATCH 예외를 반환한다")
        void failWhenSelfMatch() {
            Long sameMemberId = 10L;
            LostPetReport report = createReport(sameMemberId, 1L);
            Sighting sighting = createSighting(sameMemberId, 2L); // finder == owner
            LostPetSearchSession session = createSession(sameMemberId, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            stubHappyPath(report, session, candidate, sighting);

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, sameMemberId, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L409_SELF_MATCH.getCode()));

            verify(chatRoomDirectClient, never()).createDirectRoom(any(), any(), any(), any());
            verify(lostPetMatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("매치가 REJECTED 상태이면 L409_MATCH_CONFLICT 예외를 반환한다")
        void failWhenMatchRejected() {
            LostPetReport report = createReport(10L, 1L);
            Sighting sighting = createSighting(22L, 2L);
            LostPetSearchSession session = createSession(10L, report, 100L, LocalDateTime.now().plusHours(24));
            LostPetSearchCandidate candidate = createCandidate(session, sighting);

            LostPetMatch rejectedMatch = LostPetMatch.create(report, sighting, candidate.getScoreTotal());
            ReflectionTestUtils.setField(rejectedMatch, "status", LostPetMatchStatus.REJECTED);

            stubHappyPath(report, session, candidate, sighting);
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.of(rejectedMatch));

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L, 10L, new LostPetMatchApproveRequest(100L, 2L)
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode().getCode())
                            .isEqualTo(LostPetErrorCode.L409_MATCH_CONFLICT.getCode()));
        }
    }
}
