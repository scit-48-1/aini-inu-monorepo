package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
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
import scit.ainiinu.lostpet.domain.LostPetMatch;
import scit.ainiinu.lostpet.domain.LostPetReport;
import scit.ainiinu.lostpet.domain.LostPetSearchCandidate;
import scit.ainiinu.lostpet.domain.LostPetSearchSession;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetMatchApproveRequest;
import scit.ainiinu.lostpet.dto.LostPetMatchResponse;
import scit.ainiinu.lostpet.integration.chat.ChatDirectClientException;
import scit.ainiinu.lostpet.integration.chat.ChatDirectFailureType;
import scit.ainiinu.lostpet.integration.chat.ChatRoomDirectClient;
import scit.ainiinu.lostpet.repository.LostPetMatchRepository;
import scit.ainiinu.lostpet.repository.LostPetReportRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchCandidateRepository;
import scit.ainiinu.lostpet.repository.LostPetSearchSessionRepository;
import scit.ainiinu.lostpet.repository.SightingRepository;
import scit.ainiinu.lostpet.error.LostPetErrorCode;
import scit.ainiinu.lostpet.service.LostPetMatchApprovalService;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Nested
    @DisplayName("매치 승인")
    class Approve {

        @Test
        @DisplayName("채팅 생성 성공 시 CHAT_LINKED 상태를 반환한다")
        void chatLinked() {
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
                    new BigDecimal("0.50000"),
                    new BigDecimal("0.70000"),
                    new BigDecimal("0.81000"),
                    1
            );

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(100L, 2L))
                    .willReturn(Optional.of(candidate));
            given(sightingRepository.findById(2L)).willReturn(Optional.of(sighting));
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token")).willReturn(555L);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L,
                    10L,
                    new LostPetMatchApproveRequest(100L, 2L),
                    "Bearer test-token"
            );

            assertThat(response.status()).isEqualTo("CHAT_LINKED");
            assertThat(response.chatRoomId()).isEqualTo(555L);
        }

        @Test
        @DisplayName("채팅 생성 실패 시 PENDING_CHAT_LINK 상태를 반환한다")
        void pendingOnChatFailure() {
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
                    new BigDecimal("0.50000"),
                    new BigDecimal("0.70000"),
                    new BigDecimal("0.81000"),
                    1
            );

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(100L, 2L))
                    .willReturn(Optional.of(candidate));
            given(sightingRepository.findById(2L)).willReturn(Optional.of(sighting));
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token"))
                    .willThrow(new ChatDirectClientException(
                            ChatDirectFailureType.CONNECT,
                            "chat down"
                    ));
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L,
                    10L,
                    new LostPetMatchApproveRequest(100L, 2L),
                    "Bearer test-token"
            );

            assertThat(response.status()).isEqualTo("PENDING_CHAT_LINK");
            assertThat(response.chatRoomId()).isNull();
        }

        @Test
        @DisplayName("세션이 만료되면 예외를 반환한다")
        void failWhenSessionExpired() {
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

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L,
                    10L,
                    new LostPetMatchApproveRequest(100L, 2L),
                    "Bearer test-token"
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode().getCode())
                                .isEqualTo(LostPetErrorCode.L410_SEARCH_SESSION_EXPIRED.getCode());
                    });
        }

        @Test
        @DisplayName("세션 후보가 아니면 예외를 반환한다")
        void failWhenCandidateNotInSession() {
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
                    LocalDateTime.now().plusHours(24)
            );
            ReflectionTestUtils.setField(session, "id", 100L);

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(100L, 2L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> lostPetMatchApprovalService.approve(
                    1L,
                    10L,
                    new LostPetMatchApproveRequest(100L, 2L),
                    "Bearer test-token"
            ))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode().getCode())
                                .isEqualTo(LostPetErrorCode.L409_SEARCH_CANDIDATE_INVALID.getCode());
                    });
        }

        @Test
        @DisplayName("채팅 인증 실패 시 PENDING_CHAT_LINK 상태를 반환한다")
        void pendingOnChatAuthFailure() {
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
                    new BigDecimal("0.50000"),
                    new BigDecimal("0.70000"),
                    new BigDecimal("0.81000"),
                    1
            );

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(100L, 2L))
                    .willReturn(Optional.of(candidate));
            given(sightingRepository.findById(2L)).willReturn(Optional.of(sighting));
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token"))
                    .willThrow(new ChatDirectClientException(
                            ChatDirectFailureType.AUTH,
                            "unauthorized"
                    ));
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L,
                    10L,
                    new LostPetMatchApproveRequest(100L, 2L),
                    "Bearer test-token"
            );

            assertThat(response.status()).isEqualTo("PENDING_CHAT_LINK");
            assertThat(response.chatRoomId()).isNull();
        }

        @Test
        @DisplayName("채팅 응답 스키마 오류(null roomId) 시 PENDING_CHAT_LINK 상태를 반환한다")
        void pendingOnChatSchemaFailure() {
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
                    new BigDecimal("0.50000"),
                    new BigDecimal("0.70000"),
                    new BigDecimal("0.81000"),
                    1
            );

            given(lostPetReportRepository.findById(1L)).willReturn(Optional.of(report));
            given(lostPetSearchSessionRepository.findByIdAndOwnerIdAndLostPetReportId(100L, 10L, 1L))
                    .willReturn(Optional.of(session));
            given(lostPetSearchCandidateRepository.findBySessionIdAndSightingId(100L, 2L))
                    .willReturn(Optional.of(candidate));
            given(sightingRepository.findById(2L)).willReturn(Optional.of(sighting));
            given(lostPetMatchRepository.findByLostPetReportIdAndSightingId(1L, 2L)).willReturn(Optional.empty());
            given(chatRoomDirectClient.createDirectRoom(22L, "Bearer test-token")).willReturn(null);
            given(lostPetMatchRepository.save(any(LostPetMatch.class))).willAnswer(invocation -> invocation.getArgument(0));

            LostPetMatchResponse response = lostPetMatchApprovalService.approve(
                    1L,
                    10L,
                    new LostPetMatchApproveRequest(100L, 2L),
                    "Bearer test-token"
            );

            assertThat(response.status()).isEqualTo("PENDING_CHAT_LINK");
            assertThat(response.chatRoomId()).isNull();
        }
    }
}
