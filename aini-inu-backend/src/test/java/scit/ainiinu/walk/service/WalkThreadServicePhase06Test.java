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
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, memberId, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

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
