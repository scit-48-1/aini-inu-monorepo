package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.dto.response.WalkingSessionResponse;
import scit.ainiinu.walk.dto.response.WalkingUserResponse;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;
import scit.ainiinu.walk.exception.WalkingSessionErrorCode;
import scit.ainiinu.walk.repository.WalkingSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WalkingSessionServiceTest {

    @Mock
    private WalkingSessionRepository walkingSessionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalkingSessionService walkingSessionService;

    // ---------------------------------------------------------------
    // startSession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("산책 시작 (startSession)")
    class StartSession {

        @Test
        @DisplayName("성공: 활성 세션이 없으면 새 세션을 생성한다")
        void start_success() {
            // given
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(walkingSessionRepository.save(any(WalkingSession.class))).willAnswer(invocation -> {
                WalkingSession session = invocation.getArgument(0);
                ReflectionTestUtils.setField(session, "id", 100L);
                return session;
            });

            // when
            WalkingSessionResponse response = walkingSessionService.startSession(1L);

            // then
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.memberId()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(WalkingSessionStatus.ACTIVE);
            assertThat(response.startedAt()).isNotNull();
            assertThat(response.lastHeartbeatAt()).isNotNull();
            then(walkingSessionRepository).should().save(any(WalkingSession.class));

            // 이벤트 발행 검증
            org.mockito.ArgumentCaptor<scit.ainiinu.common.event.ContentCreatedEvent> eventCaptor =
                    org.mockito.ArgumentCaptor.forClass(scit.ainiinu.common.event.ContentCreatedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getEventType()).isEqualTo(scit.ainiinu.common.event.TimelineEventType.WALKING_SESSION_STARTED);
            assertThat(eventCaptor.getValue().getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패: 이미 활성 세션이 있으면 409 에러")
        void start_alreadyActive_fail() {
            // given
            WalkingSession existing = WalkingSession.create(1L);
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.of(existing));

            // when & then
            assertThatThrownBy(() -> walkingSessionService.startSession(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkingSessionErrorCode.WALKING_SESSION_ALREADY_ACTIVE);

            then(walkingSessionRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("성공: 다른 유저의 활성 세션이 있어도 내 세션은 생성 가능")
        void start_otherUserActive_myStartSucceeds() {
            // given
            given(walkingSessionRepository.findByMemberIdAndStatus(2L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(walkingSessionRepository.save(any(WalkingSession.class))).willAnswer(invocation -> {
                WalkingSession session = invocation.getArgument(0);
                ReflectionTestUtils.setField(session, "id", 200L);
                return session;
            });

            // when
            WalkingSessionResponse response = walkingSessionService.startSession(2L);

            // then
            assertThat(response.memberId()).isEqualTo(2L);
            assertThat(response.status()).isEqualTo(WalkingSessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("성공: ENDED 세션이 있는 유저도 새 세션을 시작할 수 있다")
        void start_endedSessionExists_success() {
            // given — findByMemberIdAndStatus(ACTIVE) returns empty (ENDED는 조회 안됨)
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(walkingSessionRepository.save(any(WalkingSession.class))).willAnswer(invocation -> {
                WalkingSession session = invocation.getArgument(0);
                ReflectionTestUtils.setField(session, "id", 300L);
                return session;
            });

            // when
            WalkingSessionResponse response = walkingSessionService.startSession(1L);

            // then
            assertThat(response.id()).isEqualTo(300L);
            assertThat(response.status()).isEqualTo(WalkingSessionStatus.ACTIVE);
        }
    }

    // ---------------------------------------------------------------
    // heartbeat
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("하트비트 (heartbeat)")
    class Heartbeat {

        @Test
        @DisplayName("성공: 활성 세션의 lastHeartbeatAt을 갱신한다")
        void heartbeat_success() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            ReflectionTestUtils.setField(session, "id", 100L);
            LocalDateTime oldHeartbeat = session.getLastHeartbeatAt();

            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.of(session));

            // when
            walkingSessionService.heartbeat(1L);

            // then
            assertThat(session.getLastHeartbeatAt()).isAfterOrEqualTo(oldHeartbeat);
        }

        @Test
        @DisplayName("실패: 활성 세션이 없으면 404 에러")
        void heartbeat_noActiveSession_fail() {
            // given
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walkingSessionService.heartbeat(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: ENDED 세션만 있으면 ACTIVE 조회 실패로 404 에러")
        void heartbeat_onlyEndedSession_fail() {
            // given
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walkingSessionService.heartbeat(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------
    // stopSession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("산책 종료 (stopSession)")
    class StopSession {

        @Test
        @DisplayName("성공: 활성 세션을 ENDED로 변경한다")
        void stop_success() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            ReflectionTestUtils.setField(session, "id", 100L);

            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.of(session));

            // when
            walkingSessionService.stopSession(1L);

            // then
            assertThat(session.getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
            assertThat(session.getEndedAt()).isNotNull();

            // 완료 이벤트 발행 검증
            org.mockito.ArgumentCaptor<scit.ainiinu.common.event.ContentCreatedEvent> eventCaptor =
                    org.mockito.ArgumentCaptor.forClass(scit.ainiinu.common.event.ContentCreatedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getEventType()).isEqualTo(scit.ainiinu.common.event.TimelineEventType.WALKING_SESSION_COMPLETED);
        }

        @Test
        @DisplayName("실패: 활성 세션이 없으면 404 에러")
        void stop_noActiveSession_fail() {
            // given
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walkingSessionService.stopSession(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 이미 종료된 세션을 다시 종료하려하면 404 에러 (ACTIVE만 조회)")
        void stop_alreadyEnded_fail() {
            // given — ENDED 세션은 findByMemberIdAndStatus(ACTIVE)에 걸리지 않음
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walkingSessionService.stopSession(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkingSessionErrorCode.WALKING_SESSION_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------
    // getActiveWalkers
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("산책중인 유저 목록 (getActiveWalkers)")
    class GetActiveWalkers {

        @Test
        @DisplayName("성공: 활성 세션이 있는 유저 목록을 반환한다")
        void getActiveWalkers_success() {
            // given
            WalkingSession session1 = WalkingSession.create(1L);
            WalkingSession session2 = WalkingSession.create(2L);
            given(walkingSessionRepository.findAllByStatus(WalkingSessionStatus.ACTIVE))
                    .willReturn(List.of(session1, session2));

            Member member1 = createMember(1L, "유저1", "img1.jpg");
            Member member2 = createMember(2L, "유저2", null);
            given(memberRepository.findAllById(List.of(1L, 2L)))
                    .willReturn(List.of(member1, member2));

            // when
            List<WalkingUserResponse> walkers = walkingSessionService.getActiveWalkers();

            // then
            assertThat(walkers).hasSize(2);
            assertThat(walkers.get(0).memberId()).isEqualTo(1L);
            assertThat(walkers.get(0).nickname()).isEqualTo("유저1");
            assertThat(walkers.get(0).profileImageUrl()).isEqualTo("img1.jpg");
            assertThat(walkers.get(0).mannerTemperature()).isNotNull();
            assertThat(walkers.get(0).walkingStartedAt()).isNotNull();
            assertThat(walkers.get(1).memberId()).isEqualTo(2L);
            assertThat(walkers.get(1).profileImageUrl()).isNull();
        }

        @Test
        @DisplayName("성공: 활성 세션이 없으면 빈 리스트를 반환한다")
        void getActiveWalkers_empty() {
            // given
            given(walkingSessionRepository.findAllByStatus(WalkingSessionStatus.ACTIVE))
                    .willReturn(List.of());

            // when
            List<WalkingUserResponse> walkers = walkingSessionService.getActiveWalkers();

            // then
            assertThat(walkers).isEmpty();
            then(memberRepository).should(never()).findAllById(any());
        }

        @Test
        @DisplayName("엣지: 세션은 있지만 해당 멤버가 삭제된 경우 해당 세션을 건너뛴다")
        void getActiveWalkers_memberDeleted_skipped() {
            // given
            WalkingSession session1 = WalkingSession.create(1L);
            WalkingSession session2 = WalkingSession.create(999L); // 존재하지 않는 멤버
            given(walkingSessionRepository.findAllByStatus(WalkingSessionStatus.ACTIVE))
                    .willReturn(List.of(session1, session2));

            Member member1 = createMember(1L, "유저1", null);
            given(memberRepository.findAllById(List.of(1L, 999L)))
                    .willReturn(List.of(member1)); // 999는 없음

            // when
            List<WalkingUserResponse> walkers = walkingSessionService.getActiveWalkers();

            // then
            assertThat(walkers).hasSize(1);
            assertThat(walkers.get(0).memberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("엣지: 여러 유저가 동시에 산책중이면 모두 반환한다")
        void getActiveWalkers_multipleUsers() {
            // given
            WalkingSession s1 = WalkingSession.create(1L);
            WalkingSession s2 = WalkingSession.create(2L);
            WalkingSession s3 = WalkingSession.create(3L);
            given(walkingSessionRepository.findAllByStatus(WalkingSessionStatus.ACTIVE))
                    .willReturn(List.of(s1, s2, s3));

            given(memberRepository.findAllById(List.of(1L, 2L, 3L)))
                    .willReturn(List.of(
                            createMember(1L, "유저1", null),
                            createMember(2L, "유저2", null),
                            createMember(3L, "유저3", null)
                    ));

            // when
            List<WalkingUserResponse> walkers = walkingSessionService.getActiveWalkers();

            // then
            assertThat(walkers).hasSize(3);
        }
    }

    // ---------------------------------------------------------------
    // getMySession
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("내 산책 세션 조회 (getMySession)")
    class GetMySession {

        @Test
        @DisplayName("성공: 활성 세션이 있으면 응답을 반환한다")
        void getMySession_active_success() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            ReflectionTestUtils.setField(session, "id", 100L);
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.of(session));

            // when
            WalkingSessionResponse response = walkingSessionService.getMySession(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(WalkingSessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("성공: 활성 세션이 없으면 null을 반환한다")
        void getMySession_noActive_returnsNull() {
            // given
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            // when
            WalkingSessionResponse response = walkingSessionService.getMySession(1L);

            // then
            assertThat(response).isNull();
        }
    }

    // ---------------------------------------------------------------
    // Entity 동작 검증
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("WalkingSession 엔티티 동작")
    class EntityBehavior {

        @Test
        @DisplayName("create()로 생성하면 ACTIVE 상태이며 시간이 설정된다")
        void create_setsInitialState() {
            // when
            WalkingSession session = WalkingSession.create(42L);

            // then
            assertThat(session.getMemberId()).isEqualTo(42L);
            assertThat(session.getStatus()).isEqualTo(WalkingSessionStatus.ACTIVE);
            assertThat(session.getStartedAt()).isNotNull();
            assertThat(session.getLastHeartbeatAt()).isNotNull();
            assertThat(session.getEndedAt()).isNull();
        }

        @Test
        @DisplayName("refreshHeartbeat()는 lastHeartbeatAt을 갱신한다")
        void refreshHeartbeat_updatesTimestamp() {
            // given
            WalkingSession session = WalkingSession.create(1L);
            LocalDateTime before = session.getLastHeartbeatAt();

            // when
            session.refreshHeartbeat();

            // then
            assertThat(session.getLastHeartbeatAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("end()는 ENDED 상태로 변경하고 endedAt을 설정한다")
        void end_setsEndedState() {
            // given
            WalkingSession session = WalkingSession.create(1L);

            // when
            session.end();

            // then
            assertThat(session.getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
            assertThat(session.getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("end()를 여러번 호출해도 예외 없이 ENDED 유지")
        void end_idempotent() {
            // given
            WalkingSession session = WalkingSession.create(1L);

            // when
            session.end();
            LocalDateTime firstEndedAt = session.getEndedAt();
            session.end();

            // then
            assertThat(session.getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
            assertThat(session.getEndedAt()).isAfterOrEqualTo(firstEndedAt);
        }
    }

    // ---------------------------------------------------------------
    // 전체 흐름 시나리오
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("전체 흐름 시나리오")
    class FullFlowScenarios {

        @Test
        @DisplayName("start → heartbeat → stop 순서로 정상 동작한다")
        void fullFlow_startHeartbeatStop() {
            // start
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());

            WalkingSession[] sessionHolder = new WalkingSession[1];
            given(walkingSessionRepository.save(any(WalkingSession.class))).willAnswer(invocation -> {
                WalkingSession s = invocation.getArgument(0);
                ReflectionTestUtils.setField(s, "id", 100L);
                sessionHolder[0] = s;
                return s;
            });

            WalkingSessionResponse startResponse = walkingSessionService.startSession(1L);
            assertThat(startResponse.status()).isEqualTo(WalkingSessionStatus.ACTIVE);

            // heartbeat
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.of(sessionHolder[0]));

            walkingSessionService.heartbeat(1L);

            // stop
            walkingSessionService.stopSession(1L);
            assertThat(sessionHolder[0].getStatus()).isEqualTo(WalkingSessionStatus.ENDED);
        }

        @Test
        @DisplayName("stop 이후 다시 start하면 새 세션이 생성된다")
        void stopThenRestart_createsNewSession() {
            // stop 이후 상태: ACTIVE 세션 없음
            given(walkingSessionRepository.findByMemberIdAndStatus(1L, WalkingSessionStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(walkingSessionRepository.save(any(WalkingSession.class))).willAnswer(invocation -> {
                WalkingSession s = invocation.getArgument(0);
                ReflectionTestUtils.setField(s, "id", 200L);
                return s;
            });

            // when
            WalkingSessionResponse response = walkingSessionService.startSession(1L);

            // then
            assertThat(response.id()).isEqualTo(200L);
            assertThat(response.status()).isEqualTo(WalkingSessionStatus.ACTIVE);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Member createMember(Long id, String nickname, String profileImageUrl) {
        Member member = Member.builder()
                .email(nickname + "@test.com")
                .nickname(nickname)
                .memberType(MemberType.PET_OWNER)
                .profileImageUrl(profileImageUrl)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
