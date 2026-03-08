package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.walk.dto.request.ThreadPatchRequest;
import scit.ainiinu.walk.dto.response.ThreadResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.ThreadErrorCode;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.walk.repository.WalkThreadApplicationPetRepository;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadFilterRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WalkThreadServicePhase06Test {

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @Mock
    private WalkThreadPetRepository walkThreadPetRepository;

    @Mock
    private WalkThreadFilterRepository walkThreadFilterRepository;

    @Mock
    private WalkThreadApplicationRepository walkThreadApplicationRepository;

    @Mock
    private WalkThreadApplicationPetRepository walkThreadApplicationPetRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalkThreadService walkThreadService;

    @Nested
    @DisplayName("스레드 수정")
    class UpdateThread {

        @Test
        @DisplayName("같은 펫으로 수정 시 flush 호출 후 정상 저장")
        void update_samePets_flushAndSave() {
            // given
            Long memberId = 1L;
            Long threadId = 10L;

            WalkThread thread = buildThread(threadId, memberId);

            ThreadPatchRequest request = new ThreadPatchRequest();
            request.setTitle("수정된 제목");
            request.setPetIds(List.of(1L)); // same pet

            given(walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED))
                    .willReturn(Optional.of(thread));
            given(walkThreadPetRepository.findAllByThreadId(threadId))
                    .willReturn(List.of(WalkThreadPet.of(threadId, 1L)));
            given(walkThreadRepository.saveAndFlush(thread)).willReturn(thread);
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(threadId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(threadId, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            ThreadResponse response = walkThreadService.updateThread(memberId, threadId, request);

            // then
            then(walkThreadPetRepository).should().deleteAllByThreadId(threadId);
            then(walkThreadPetRepository).should().flush();
            then(walkThreadPetRepository).should().save(any(WalkThreadPet.class));
            assertThat(response.getTitle()).isEqualTo("수정된 제목");
        }

        @Test
        @DisplayName("다른 펫으로 수정 시 기존 삭제 후 신규 저장")
        void update_differentPets_deleteAndSaveNew() {
            // given
            Long memberId = 1L;
            Long threadId = 10L;

            WalkThread thread = buildThread(threadId, memberId);

            ThreadPatchRequest request = new ThreadPatchRequest();
            request.setPetIds(List.of(2L, 3L)); // different pets

            given(walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED))
                    .willReturn(Optional.of(thread));
            given(walkThreadPetRepository.findAllByThreadId(threadId))
                    .willReturn(List.of(WalkThreadPet.of(threadId, 2L), WalkThreadPet.of(threadId, 3L)));
            given(walkThreadRepository.saveAndFlush(thread)).willReturn(thread);
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(threadId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(threadId, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            walkThreadService.updateThread(memberId, threadId, request);

            // then
            then(walkThreadPetRepository).should().deleteAllByThreadId(threadId);
            then(walkThreadPetRepository).should().flush();
            then(walkThreadPetRepository).should(times(2)).save(any(WalkThreadPet.class));
        }

        @Test
        @DisplayName("작성자가 아닌 사용자의 수정 시도 시 THREAD_OWNER_ONLY 예외")
        void update_nonOwner_throwsException() {
            // given
            Long authorId = 1L;
            Long otherMemberId = 99L;
            Long threadId = 10L;

            WalkThread thread = buildThread(threadId, authorId);

            ThreadPatchRequest request = new ThreadPatchRequest();
            request.setTitle("해킹 시도");

            given(walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED))
                    .willReturn(Optional.of(thread));

            // when & then
            assertThatThrownBy(() -> walkThreadService.updateThread(otherMemberId, threadId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ThreadErrorCode.THREAD_OWNER_ONLY);
        }
    }

    @Nested
    @DisplayName("스레드 목록 조회")
    class GetThreads {

        @Test
        @DisplayName("날짜 필터 없이 조회 시 findByStatusOrderByCreatedAtDescIdDesc 호출")
        void getThreads_noDateFilter_usesDefaultQuery() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            WalkThread thread = buildFutureThread(1L, 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);
            given(walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus.RECRUITING, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, null, null, null, null, null);

            // then
            then(walkThreadRepository).should().findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus.RECRUITING, pageable);
            then(walkThreadRepository).should(never()).findByStatusAndWalkDateRange(any(), any(), any(), any());
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("startDate만 있을 때 effectiveEnd는 2099-12-31")
        void getThreads_onlyStartDate_effectiveEndIs2099() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            WalkThread thread = buildFutureThread(1L, 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);
            given(walkThreadRepository.findByStatusAndWalkDateRange(
                    WalkThreadStatus.RECRUITING, startDate, LocalDate.of(2099, 12, 31), pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, startDate, null, null, null, null);

            // then
            then(walkThreadRepository).should().findByStatusAndWalkDateRange(
                    WalkThreadStatus.RECRUITING, startDate, LocalDate.of(2099, 12, 31), pageable);
        }

        @Test
        @DisplayName("endDate만 있을 때 effectiveStart는 2000-01-01")
        void getThreads_onlyEndDate_effectiveStartIs2000() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate endDate = LocalDate.of(2026, 12, 31);
            WalkThread thread = buildFutureThread(1L, 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);
            given(walkThreadRepository.findByStatusAndWalkDateRange(
                    WalkThreadStatus.RECRUITING, LocalDate.of(2000, 1, 1), endDate, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, null, endDate, null, null, null);

            // then
            then(walkThreadRepository).should().findByStatusAndWalkDateRange(
                    WalkThreadStatus.RECRUITING, LocalDate.of(2000, 1, 1), endDate, pageable);
        }

        @Test
        @DisplayName("startDate와 endDate 둘 다 있을 때 findByStatusAndWalkDateRange 호출")
        void getThreads_bothDates_usesDateRangeQuery() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate startDate = LocalDate.of(2026, 3, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);
            WalkThread thread = buildFutureThread(1L, 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);
            given(walkThreadRepository.findByStatusAndWalkDateRange(
                    WalkThreadStatus.RECRUITING, startDate, endDate, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, startDate, endDate, null, null, null);

            // then
            then(walkThreadRepository).should().findByStatusAndWalkDateRange(
                    WalkThreadStatus.RECRUITING, startDate, endDate, pageable);
            then(walkThreadRepository).should(never()).findByStatusOrderByCreatedAtDescIdDesc(any(), any());
        }

        @Test
        @DisplayName("위치 필터 적용 시 반경 내 스레드만 반환")
        void getThreads_locationFilter_onlyWithinRadius() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            // Seoul Forest (center)
            double centerLat = 37.5445;
            double centerLng = 127.0445;
            double radiusKm = 10.0;

            // Gangnam Station ~5.3km away (within radius)
            WalkThread nearThread = buildFutureThreadWithLocation(1L, 2L,
                    BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276));

            // Incheon ~30km away (outside radius)
            WalkThread farThread = buildFutureThreadWithLocation(2L, 3L,
                    BigDecimal.valueOf(37.4563), BigDecimal.valueOf(126.7052));

            Slice<WalkThread> slice = new SliceImpl<>(List.of(nearThread, farThread), pageable, false);
            given(walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus.RECRUITING, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(
                    memberId, pageable, null, null, centerLat, centerLng, radiusKm);

            // then - only near thread within 10km radius
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("만료된 스레드는 목록에서 제외")
        void getThreads_expiredThreads_excluded() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            WalkThread activeThread = buildFutureThread(1L, 2L);

            // Expired thread: startTime in the past (more than 60 minutes ago)
            WalkThread expiredThread = WalkThread.builder()
                    .authorId(3L)
                    .title("만료된 모집")
                    .description("이미 지난 모집")
                    .walkDate(LocalDate.now().minusDays(1))
                    .startTime(LocalDateTime.now().minusHours(3))
                    .endTime(LocalDateTime.now().minusHours(2))
                    .chatType(WalkChatType.GROUP)
                    .maxParticipants(5)
                    .allowNonPetOwner(true)
                    .isVisibleAlways(true)
                    .placeName("서울숲")
                    .latitude(BigDecimal.valueOf(37.5445))
                    .longitude(BigDecimal.valueOf(127.0445))
                    .address("성동구")
                    .status(WalkThreadStatus.RECRUITING)
                    .build();
            ReflectionTestUtils.setField(expiredThread, "id", 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(activeThread, expiredThread), pageable, false);
            given(walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus.RECRUITING, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, null, null, null, null, null);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("내 활성 스레드 조회")
    class GetMyActiveThread {

        @Test
        @DisplayName("활성 스레드가 있으면 반환")
        void getMyActiveThread_hasActive_returns() {
            // given
            Long memberId = 1L;
            WalkThread thread = buildFutureThread(1L, memberId);

            given(walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyActiveThread(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("만료된 스레드는 제외")
        void getMyActiveThread_expiredExcluded() {
            // given
            Long memberId = 1L;

            WalkThread expiredThread = WalkThread.builder()
                    .authorId(memberId)
                    .title("만료된 모집")
                    .description("이미 지난 모집")
                    .walkDate(LocalDate.now().minusDays(1))
                    .startTime(LocalDateTime.now().minusHours(3))
                    .endTime(LocalDateTime.now().minusHours(2))
                    .chatType(WalkChatType.GROUP)
                    .maxParticipants(5)
                    .allowNonPetOwner(true)
                    .isVisibleAlways(true)
                    .placeName("서울숲")
                    .latitude(BigDecimal.valueOf(37.5445))
                    .longitude(BigDecimal.valueOf(127.0445))
                    .address("성동구")
                    .status(WalkThreadStatus.RECRUITING)
                    .build();
            ReflectionTestUtils.setField(expiredThread, "id", 1L);

            given(walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(expiredThread));

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyActiveThread(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("스레드 없으면 빈 리스트 반환")
        void getMyActiveThread_noThreads_emptyList() {
            // given
            Long memberId = 1L;
            given(walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING))
                    .willReturn(List.of());

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyActiveThread(memberId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("applied 필드")
    class AppliedField {

        @Test
        @DisplayName("이미 신청한 사용자는 applied = true")
        void getThread_appliedUser_appliedTrue() {
            // given
            Long memberId = 5L;
            Long threadId = 10L;

            WalkThread thread = buildFutureThread(threadId, 1L); // author is 1L

            WalkThreadApplication application = WalkThreadApplication.joined(threadId, memberId, 100L);

            given(walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED))
                    .willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(threadId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(1L);
            given(walkThreadPetRepository.findAllByThreadId(threadId))
                    .willReturn(List.of(WalkThreadPet.of(threadId, 1L)));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(threadId, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.of(application));

            // when
            ThreadResponse response = walkThreadService.getThread(memberId, threadId);

            // then
            assertThat(response.getApplied()).isTrue();
        }

        @Test
        @DisplayName("신청하지 않은 사용자는 applied = false")
        void getThread_nonAppliedUser_appliedFalse() {
            // given
            Long memberId = 5L;
            Long threadId = 10L;

            WalkThread thread = buildFutureThread(threadId, 1L);

            given(walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED))
                    .willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(threadId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadPetRepository.findAllByThreadId(threadId))
                    .willReturn(List.of(WalkThreadPet.of(threadId, 1L)));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(threadId, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            ThreadResponse response = walkThreadService.getThread(memberId, threadId);

            // then
            assertThat(response.getApplied()).isFalse();
        }
    }

    @Nested
    @DisplayName("지도용 모집글 날짜 필터")
    class GetMapThreads {

        @Test
        @DisplayName("startDate/endDate 필터 적용 시 범위 내 스레드만 반환")
        void getMapThreads_withDateFilter_returnsOnlyMatchingDates() {
            // given
            Long memberId = 1L;
            double lat = 37.5445, lng = 127.0445, radiusKm = 10.0;

            // Thread with walkDate = today + 2 days
            WalkThread thread1 = buildFutureThreadWithWalkDate(1L, 2L, LocalDate.now().plusDays(2));
            // Thread with walkDate = today + 10 days
            WalkThread thread2 = buildFutureThreadWithWalkDate(2L, 3L, LocalDate.now().plusDays(10));

            given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread1, thread2));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L))).willReturn(List.of());

            // Date range covers only thread1 (today to today+5)
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(5);

            // when
            List<scit.ainiinu.walk.dto.response.ThreadMapResponse> result =
                    walkThreadService.getMapThreads(memberId, lat, lng, radiusKm, startDate, endDate);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getThreadId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("날짜 필터 null이면 모든 비만료 스레드 반환 (하위 호환)")
        void getMapThreads_noDateFilter_returnsAll() {
            // given
            Long memberId = 1L;
            double lat = 37.5445, lng = 127.0445, radiusKm = 10.0;

            WalkThread thread1 = buildFutureThreadWithWalkDate(1L, 2L, LocalDate.now().plusDays(2));
            WalkThread thread2 = buildFutureThreadWithWalkDate(2L, 3L, LocalDate.now().plusDays(10));

            given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread1, thread2));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L, 2L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L, 2L))).willReturn(List.of());

            // when
            List<scit.ainiinu.walk.dto.response.ThreadMapResponse> result =
                    walkThreadService.getMapThreads(memberId, lat, lng, radiusKm, null, null);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("내 참여 중인 산책 조회")
    class GetMyJoinedThreads {

        @Test
        @DisplayName("JOINED 상태의 RECRUITING 스레드만 반환한다")
        void getMyJoinedThreads_returnsOnlyRecruitingJoinedThreads() {
            // given
            Long memberId = 5L;
            WalkThread recruitingThread = buildFutureThread(10L, 1L); // author=1, not memberId
            WalkThread completedThread = buildFutureThread(20L, 2L);
            ReflectionTestUtils.setField(completedThread, "status", WalkThreadStatus.COMPLETED);

            WalkThreadApplication app1 = WalkThreadApplication.joined(10L, memberId, 100L);
            WalkThreadApplication app2 = WalkThreadApplication.joined(20L, memberId, 200L);

            given(walkThreadApplicationRepository.findAllByMemberIdAndStatus(memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(app1, app2));
            given(walkThreadRepository.findAllById(List.of(10L, 20L)))
                    .willReturn(List.of(recruitingThread, completedThread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(10L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.<Object[]>of(new Object[]{10L, 1L}));
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(10L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(app1));
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(10L)))
                    .willReturn(List.of());

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyJoinedThreads(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(10L);
            assertThat(result.get(0).isApplied()).isTrue();
        }

        @Test
        @DisplayName("만료된 스레드는 제외한다")
        void getMyJoinedThreads_excludesExpired() {
            // given
            Long memberId = 5L;
            WalkThread expiredThread = WalkThread.builder()
                    .authorId(1L)
                    .title("만료된 모집")
                    .description("이미 지난 모집")
                    .walkDate(LocalDate.now().minusDays(1))
                    .startTime(LocalDateTime.now().minusHours(3))
                    .endTime(LocalDateTime.now().minusHours(2))
                    .chatType(WalkChatType.GROUP)
                    .maxParticipants(5)
                    .allowNonPetOwner(true)
                    .isVisibleAlways(true)
                    .placeName("서울숲")
                    .latitude(BigDecimal.valueOf(37.5445))
                    .longitude(BigDecimal.valueOf(127.0445))
                    .address("성동구")
                    .status(WalkThreadStatus.RECRUITING)
                    .build();
            ReflectionTestUtils.setField(expiredThread, "id", 10L);

            WalkThreadApplication app = WalkThreadApplication.joined(10L, memberId, 100L);

            given(walkThreadApplicationRepository.findAllByMemberIdAndStatus(memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(app));
            given(walkThreadRepository.findAllById(List.of(10L)))
                    .willReturn(List.of(expiredThread));

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyJoinedThreads(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("자신이 작성한 스레드는 제외한다")
        void getMyJoinedThreads_excludesOwnThreads() {
            // given
            Long memberId = 5L;
            WalkThread ownThread = buildFutureThread(10L, memberId); // author == memberId

            WalkThreadApplication app = WalkThreadApplication.joined(10L, memberId, 100L);

            given(walkThreadApplicationRepository.findAllByMemberIdAndStatus(memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(app));
            given(walkThreadRepository.findAllById(List.of(10L)))
                    .willReturn(List.of(ownThread));

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyJoinedThreads(memberId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("참여 신청이 없으면 빈 리스트를 반환한다")
        void getMyJoinedThreads_noApplications_emptyList() {
            // given
            Long memberId = 5L;
            given(walkThreadApplicationRepository.findAllByMemberIdAndStatus(memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyJoinedThreads(memberId);

            // then
            assertThat(result).isEmpty();
            then(walkThreadRepository).should(never()).findAllById(any());
        }
    }

    @Nested
    @DisplayName("ThreadSummaryResponse petImageUrl")
    class SummaryPetImageUrl {

        @Test
        @DisplayName("getMyActiveThread: 반려견 사진이 있으면 petImageUrl에 포함된다")
        void getMyActiveThread_withPetPhoto_returnsPetImageUrl() {
            // given
            Long memberId = 1L;
            WalkThread thread = buildFutureThread(1L, memberId);

            given(walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());

            WalkThreadPet threadPet = WalkThreadPet.of(1L, 100L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of(threadPet));

            Pet pet = Pet.builder().memberId(memberId).build();
            ReflectionTestUtils.setField(pet, "id", 100L);
            ReflectionTestUtils.setField(pet, "photoUrl", "https://cdn.example.com/dog.jpg");
            given(petRepository.findAllById(List.of(100L)))
                    .willReturn(List.of(pet));

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyActiveThread(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetImageUrl()).isEqualTo("https://cdn.example.com/dog.jpg");
        }

        @Test
        @DisplayName("getMyActiveThread: 반려견이 없으면 petImageUrl이 null이다")
        void getMyActiveThread_noPet_nullPetImageUrl() {
            // given
            Long memberId = 1L;
            WalkThread thread = buildFutureThread(1L, memberId);

            given(walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyActiveThread(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetImageUrl()).isNull();
        }

        @Test
        @DisplayName("getMyJoinedThreads: 반려견 사진이 있으면 petImageUrl에 포함된다")
        void getMyJoinedThreads_withPetPhoto_returnsPetImageUrl() {
            // given
            Long memberId = 5L;
            WalkThread thread = buildFutureThread(10L, 1L);

            WalkThreadApplication app = WalkThreadApplication.joined(10L, memberId, 100L);

            given(walkThreadApplicationRepository.findAllByMemberIdAndStatus(memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(app));
            given(walkThreadRepository.findAllById(List.of(10L)))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(10L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.<Object[]>of(new Object[]{10L, 1L}));
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(10L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(app));

            WalkThreadPet threadPet = WalkThreadPet.of(10L, 200L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(10L)))
                    .willReturn(List.of(threadPet));

            Pet pet = Pet.builder().memberId(1L).build();
            ReflectionTestUtils.setField(pet, "id", 200L);
            ReflectionTestUtils.setField(pet, "photoUrl", "https://cdn.example.com/joined-pet.jpg");
            given(petRepository.findAllById(List.of(200L)))
                    .willReturn(List.of(pet));

            // when
            List<ThreadSummaryResponse> result = walkThreadService.getMyJoinedThreads(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetImageUrl()).isEqualTo("https://cdn.example.com/joined-pet.jpg");
        }

        @Test
        @DisplayName("getThreads: 반려견 사진이 있으면 petImageUrl에 포함된다")
        void getThreads_withPetPhoto_returnsPetImageUrl() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            WalkThread thread = buildFutureThread(1L, 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);
            given(walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus.RECRUITING, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());

            WalkThreadPet threadPet = WalkThreadPet.of(1L, 100L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of(threadPet));

            Pet pet = Pet.builder().memberId(2L).build();
            ReflectionTestUtils.setField(pet, "id", 100L);
            ReflectionTestUtils.setField(pet, "photoUrl", "https://cdn.example.com/list-pet.jpg");
            given(petRepository.findAllById(List.of(100L)))
                    .willReturn(List.of(pet));

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, null, null, null, null, null);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getPetImageUrl()).isEqualTo("https://cdn.example.com/list-pet.jpg");
        }

        @Test
        @DisplayName("getThreads: 반려견 photoUrl이 null이면 petImageUrl도 null이다")
        void getThreads_petPhotoNull_nullPetImageUrl() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            WalkThread thread = buildFutureThread(1L, 2L);

            Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);
            given(walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(WalkThreadStatus.RECRUITING, pageable))
                    .willReturn(slice);
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(1L), WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(1L), memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());

            WalkThreadPet threadPet = WalkThreadPet.of(1L, 100L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of(threadPet));

            Pet pet = Pet.builder().memberId(2L).build();
            ReflectionTestUtils.setField(pet, "id", 100L);
            // photoUrl is null by default
            given(petRepository.findAllById(List.of(100L)))
                    .willReturn(List.of(pet));

            // when
            SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, null, null, null, null, null);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getPetImageUrl()).isNull();
        }
    }

    // --- Helper methods ---

    private WalkThread buildThread(Long threadId, Long authorId) {
        WalkThread thread = WalkThread.builder()
                .authorId(authorId)
                .title("한강 산책 모집")
                .description("저녁 산책 함께해요")
                .walkDate(LocalDate.now().plusDays(1))
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(BigDecimal.valueOf(37.5445))
                .longitude(BigDecimal.valueOf(127.0445))
                .address("성동구")
                .status(WalkThreadStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        return thread;
    }

    private WalkThread buildFutureThread(Long threadId, Long authorId) {
        return buildFutureThreadWithLocation(threadId, authorId,
                BigDecimal.valueOf(37.5445), BigDecimal.valueOf(127.0445));
    }

    private WalkThread buildFutureThreadWithWalkDate(Long threadId, Long authorId, LocalDate walkDate) {
        WalkThread thread = WalkThread.builder()
                .authorId(authorId)
                .title("산책 모집 " + threadId)
                .description("산책 함께해요")
                .walkDate(walkDate)
                .startTime(walkDate.atTime(14, 0))
                .endTime(walkDate.atTime(15, 0))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(BigDecimal.valueOf(37.5445))
                .longitude(BigDecimal.valueOf(127.0445))
                .address("성동구")
                .status(WalkThreadStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        return thread;
    }

    private WalkThread buildFutureThreadWithLocation(Long threadId, Long authorId,
                                                      BigDecimal latitude, BigDecimal longitude) {
        WalkThread thread = WalkThread.builder()
                .authorId(authorId)
                .title("한강 산책 모집")
                .description("저녁 산책 함께해요")
                .walkDate(LocalDate.now().plusDays(1))
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(latitude)
                .longitude(longitude)
                .address("성동구")
                .status(WalkThreadStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        return thread;
    }
}
