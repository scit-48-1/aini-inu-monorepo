package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
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
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.dto.request.ThreadPatchRequest;
import scit.ainiinu.walk.dto.response.ThreadHotspotResponse;
import scit.ainiinu.walk.dto.response.ThreadMapResponse;
import scit.ainiinu.walk.dto.response.ThreadResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WalkThreadServiceCoverageTest {

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

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalkThreadService walkThreadService;

    @Test
    @DisplayName("스레드 목록 조회 시 현재 참여 인원과 신청 여부를 포함해 반환한다")
    void getThreads_returnsSummarySlice() {
        // given
        WalkThread thread = recruitingThread(101L, 10L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(3));
        Pageable pageable = PageRequest.of(0, 10);
        Slice<WalkThread> slice = new SliceImpl<>(List.of(thread), pageable, false);

        given(walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(eq(WalkThreadStatus.RECRUITING), any(Pageable.class)))
                .willReturn(slice);
        given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(101L), WalkThreadApplicationStatus.JOINED))
                .willReturn(List.<Object[]>of(new Object[]{101L, 2L}));
        given(walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(List.of(101L), 1L, WalkThreadApplicationStatus.JOINED))
                .willReturn(List.of(WalkThreadApplication.joined(101L, 1L, 9001L)));

        // when
        SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(1L, pageable, null, null, null, null, null);

        // then
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(101L);
        assertThat(response.getContent().get(0).getCurrentParticipants()).isEqualTo(3);
        assertThat(response.getContent().get(0).isApplied()).isTrue();
    }

    @Test
    @DisplayName("지도 조회는 반경 내 모집중 스레드만 반환한다")
    void getMapThreads_filtersByDistanceAndExpiry() {
        // given
        WalkThread near = recruitingThread(101L, 10L, "시청", 37.5663, 126.9779, LocalDateTime.now().plusHours(2));
        WalkThread far = recruitingThread(102L, 10L, "부산", 35.1796, 129.0756, LocalDateTime.now().plusHours(2));
        WalkThread expired = recruitingThread(103L, 10L, "강남", 37.4979, 127.0276, LocalDateTime.now().minusHours(3));

        given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING)).willReturn(List.of(near, far, expired));
        given(walkThreadApplicationRepository.countByThreadIdInAndStatus(List.of(101L), WalkThreadApplicationStatus.JOINED))
                .willReturn(List.<Object[]>of(new Object[]{101L, 3L}));

        // when
        List<ThreadMapResponse> responses = walkThreadService.getMapThreads(1L, 37.5663, 126.9779, 5.0, null, null);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getThreadId()).isEqualTo(101L);
        assertThat(responses.get(0).getCurrentParticipants()).isEqualTo(4);
    }

    @Test
    @DisplayName("스레드 상세 조회 시 작성자는 신청자 목록을 확인할 수 있다")
    void getThread_authorIncludesApplicants() {
        // given
        WalkThread thread = recruitingThread(1L, 1L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(2));
        WalkThreadApplication application = WalkThreadApplication.joined(1L, 2L, 7001L);

        given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
        given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(1L);
        given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(List.of(WalkThreadPet.of(1L, 1001L)));
        given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                .willReturn(List.of(application));

        // when
        ThreadResponse response = walkThreadService.getThread(1L, 1L);

        // then
        assertThat(response.getPetIds()).containsExactly(1001L);
        assertThat(response.getApplicants()).hasSize(1);
        assertThat(response.getApplicants().get(0).getMemberId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("작성자는 스레드 제목/설명을 수정할 수 있다")
    void updateThread_ownerCanUpdate() {
        // given
        WalkThread thread = recruitingThread(1L, 1L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(2));
        ThreadPatchRequest request = new ThreadPatchRequest();
        request.setTitle("수정된 제목");
        request.setDescription("수정된 설명");

        given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
        given(walkThreadRepository.saveAndFlush(any(WalkThread.class))).willAnswer(inv -> inv.getArgument(0));
        given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
        given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(List.of());
        given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                .willReturn(List.of());

        // when
        ThreadResponse response = walkThreadService.updateThread(1L, 1L, request);

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getDescription()).isEqualTo("수정된 설명");
        then(walkThreadRepository).should().saveAndFlush(any(WalkThread.class));
    }

    @Test
    @DisplayName("petIds와 함께 제목/날짜/채팅유형을 수정하면 saveAndFlush 이후 pet을 교체한다")
    void updateThread_withPetIds_flushesBeforePetDelete() {
        // given
        WalkThread thread = recruitingThread(1L, 1L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(2));
        ThreadPatchRequest request = new ThreadPatchRequest();
        request.setTitle("변경된 제목");
        request.setDescription("변경된 설명");
        request.setWalkDate(LocalDate.now().plusDays(3));
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        request.setChatType("INDIVIDUAL");
        request.setMaxParticipants(2);
        request.setPetIds(List.of(201L, 202L));

        given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
        given(walkThreadRepository.saveAndFlush(any(WalkThread.class))).willAnswer(inv -> inv.getArgument(0));
        given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
        given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(
                List.of(WalkThreadPet.of(1L, 201L), WalkThreadPet.of(1L, 202L)));
        given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                .willReturn(List.of());

        // when
        ThreadResponse response = walkThreadService.updateThread(1L, 1L, request);

        // then — field changes reflected in response
        assertThat(response.getTitle()).isEqualTo("변경된 제목");
        assertThat(response.getDescription()).isEqualTo("변경된 설명");
        assertThat(response.getChatType()).isEqualTo("INDIVIDUAL");
        assertThat(response.getWalkDate()).isEqualTo(LocalDate.now().plusDays(3));

        // then — saveAndFlush called BEFORE pet delete
        var inOrder = org.mockito.Mockito.inOrder(walkThreadRepository, walkThreadPetRepository);
        inOrder.verify(walkThreadRepository).saveAndFlush(any(WalkThread.class));
        inOrder.verify(walkThreadPetRepository).deleteAllByThreadId(1L);
    }

    @Test
    @DisplayName("작성자가 스레드를 삭제하면 상태가 DELETED로 변경된다")
    void deleteThread_ownerMarksDeleted() {
        // given
        WalkThread thread = recruitingThread(1L, 1L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(2));
        given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));

        // when
        walkThreadService.deleteThread(1L, 1L);

        // then
        assertThat(thread.getStatus()).isEqualTo(WalkThreadStatus.DELETED);

        // 삭제 이벤트 발행 검증
        org.mockito.ArgumentCaptor<scit.ainiinu.common.event.ContentDeletedEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(scit.ainiinu.common.event.ContentDeletedEvent.class);
        org.mockito.BDDMockito.then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(scit.ainiinu.common.event.TimelineEventType.WALK_THREAD_CREATED);
        assertThat(eventCaptor.getValue().getReferenceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("신청 취소 시 상태가 CANCELED로 변경된다")
    void cancelApplyThread_marksCanceled() {
        // given
        WalkThreadApplication application = WalkThreadApplication.joined(1L, 2L, 9001L);
        given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, 2L, WalkThreadApplicationStatus.JOINED))
                .willReturn(Optional.of(application));

        // when
        walkThreadService.cancelApplyThread(2L, 1L);

        // then
        assertThat(application.getStatus()).isEqualTo(WalkThreadApplicationStatus.CANCELED);
    }

    @Test
    @DisplayName("핫스팟 조회는 지역별 모집 수를 집계해 내림차순으로 반환한다")
    void getHotspots_groupsAndSortsByCount() {
        // given
        WalkThread seoul1 = recruitingThread(1L, 10L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(2));
        WalkThread seoul2 = recruitingThread(2L, 11L, "서울숲", 37.5459, 127.0405, LocalDateTime.now().plusHours(3));
        WalkThread gangnam = recruitingThread(3L, 12L, "강남", 37.4979, 127.0276, LocalDateTime.now().plusHours(2));

        given(walkThreadRepository.findByStatusAndCreatedAfter(eq(WalkThreadStatus.RECRUITING), any(LocalDateTime.class)))
                .willReturn(List.of(seoul1, gangnam, seoul2));

        // when
        List<ThreadHotspotResponse> responses = walkThreadService.getHotspots(6);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRegion()).isEqualTo("서울숲");
        assertThat(responses.get(0).getCount()).isEqualTo(2L);
        assertThat(responses.get(1).getRegion()).isEqualTo("강남");
        assertThat(responses.get(1).getCount()).isEqualTo(1L);
    }

    private WalkThread recruitingThread(
            Long id,
            Long authorId,
            String placeName,
            double latitude,
            double longitude,
            LocalDateTime startTime
    ) {
        WalkThread thread = WalkThread.builder()
                .authorId(authorId)
                .title("산책 모집")
                .description("함께 산책해요")
                .walkDate(LocalDate.now().plusDays(1))
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName(placeName)
                .latitude(BigDecimal.valueOf(latitude))
                .longitude(BigDecimal.valueOf(longitude))
                .address("서울시")
                .status(WalkThreadStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(thread, "id", id);
        return thread;
    }
}
