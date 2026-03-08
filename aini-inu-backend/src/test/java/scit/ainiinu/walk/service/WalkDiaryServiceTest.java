package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.ContentDeletedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.AvailableThreadResponse;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WalkDiaryServiceTest {

    @Mock
    private WalkDiaryRepository walkDiaryRepository;

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @Mock
    private WalkThreadApplicationRepository walkThreadApplicationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalkDiaryService walkDiaryService;

    @Nested
    @DisplayName("일기 생성")
    class CreateDiary {

        @Test
        @DisplayName("공개 범위 미입력 시 기본값은 PUBLIC(true)다")
        void create_defaultPublic_success() {
            // given
            Long memberId = 1L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("한강 산책 일기");
            request.setContent("오늘 날씨가 좋았다");
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("https://cdn/1.jpg"));
            request.setIsPublic(null);

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createCompletedThread(memberId, threadId)));
            given(walkDiaryRepository.existsByMemberIdAndThreadIdAndDeletedAtIsNull(memberId, threadId)).willReturn(false);
            given(walkDiaryRepository.save(any(WalkDiary.class))).willAnswer(invocation -> {
                WalkDiary diary = invocation.getArgument(0);
                ReflectionTestUtils.setField(diary, "id", 1L);
                return diary;
            });

            // when
            WalkDiaryResponse response = walkDiaryService.createDiary(memberId, request);

            // then
            assertThat(response.isPublic()).isTrue();
            ArgumentCaptor<WalkDiary> captor = ArgumentCaptor.forClass(WalkDiary.class);
            then(walkDiaryRepository).should().save(captor.capture());
            assertThat(captor.getValue().getIsPublic()).isTrue();
        }

        @Test
        @DisplayName("일기 생성 시 ContentCreatedEvent가 발행된다")
        void create_publishesContentCreatedEvent() {
            // given
            Long memberId = 1L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("산책 일기");
            request.setContent("내용");
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("https://cdn/thumb.jpg"));

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createCompletedThread(memberId, threadId)));
            given(walkDiaryRepository.existsByMemberIdAndThreadIdAndDeletedAtIsNull(memberId, threadId)).willReturn(false);
            given(walkDiaryRepository.save(any(WalkDiary.class))).willAnswer(invocation -> {
                WalkDiary diary = invocation.getArgument(0);
                ReflectionTestUtils.setField(diary, "id", 5L);
                return diary;
            });

            // when
            walkDiaryService.createDiary(memberId, request);

            // then
            ArgumentCaptor<ContentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ContentCreatedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());

            ContentCreatedEvent event = eventCaptor.getValue();
            assertThat(event.getMemberId()).isEqualTo(memberId);
            assertThat(event.getReferenceId()).isEqualTo(5L);
            assertThat(event.getEventType()).isEqualTo(TimelineEventType.WALK_DIARY_CREATED);
            assertThat(event.getThumbnailUrl()).isEqualTo("https://cdn/thumb.jpg");
        }
    }

    @Nested
    @DisplayName("일기 생성 검증")
    class CreateDiaryValidation {

        @Test
        @DisplayName("COMPLETED 스레드 + 참여자(author) → 성공")
        void create_completedThread_author_success() {
            // given
            Long memberId = 1L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("산책 일기");
            request.setContent("내용");
            request.setWalkDate(LocalDate.now());

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createCompletedThread(memberId, threadId)));
            given(walkDiaryRepository.existsByMemberIdAndThreadIdAndDeletedAtIsNull(memberId, threadId)).willReturn(false);
            given(walkDiaryRepository.save(any(WalkDiary.class))).willAnswer(invocation -> {
                WalkDiary diary = invocation.getArgument(0);
                ReflectionTestUtils.setField(diary, "id", 1L);
                return diary;
            });

            // when
            WalkDiaryResponse response = walkDiaryService.createDiary(memberId, request);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("RECRUITING 상태 스레드 → THREAD_NOT_COMPLETED 예외")
        void create_recruitingThread_fail() {
            // given
            Long memberId = 1L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("산책 일기");
            request.setContent("내용");
            request.setWalkDate(LocalDate.now());

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createThreadWithStatus(memberId, threadId, WalkThreadStatus.RECRUITING)));

            // when & then
            assertThatThrownBy(() -> walkDiaryService.createDiary(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.THREAD_NOT_COMPLETED);
        }

        @Test
        @DisplayName("EXPIRED 상태 스레드 → THREAD_NOT_COMPLETED 예외")
        void create_expiredThread_fail() {
            // given
            Long memberId = 1L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("산책 일기");
            request.setContent("내용");
            request.setWalkDate(LocalDate.now());

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createThreadWithStatus(memberId, threadId, WalkThreadStatus.EXPIRED)));

            // when & then
            assertThatThrownBy(() -> walkDiaryService.createDiary(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.THREAD_NOT_COMPLETED);
        }

        @Test
        @DisplayName("스레드 참여자가 아닌 경우 → NOT_THREAD_PARTICIPANT 예외")
        void create_notParticipant_fail() {
            // given
            Long memberId = 99L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("산책 일기");
            request.setContent("내용");
            request.setWalkDate(LocalDate.now());

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createCompletedThread(1L, threadId)));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(
                    eq(threadId), eq(memberId), eq(WalkThreadApplicationStatus.JOINED)))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walkDiaryService.createDiary(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.NOT_THREAD_PARTICIPANT);
        }

        @Test
        @DisplayName("동일 스레드에 이미 일기 존재 → DIARY_ALREADY_EXISTS 예외")
        void create_alreadyExists_fail() {
            // given
            Long memberId = 1L;
            Long threadId = 100L;
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setThreadId(threadId);
            request.setTitle("산책 일기");
            request.setContent("내용");
            request.setWalkDate(LocalDate.now());

            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(createCompletedThread(memberId, threadId)));
            given(walkDiaryRepository.existsByMemberIdAndThreadIdAndDeletedAtIsNull(memberId, threadId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> walkDiaryService.createDiary(memberId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.DIARY_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("작성가능 스레드 조회")
    class GetAvailableThreads {

        @Test
        @DisplayName("COMPLETED 스레드 중 일기 미작성만 반환한다")
        void availableThreads_excludeAlreadyWritten() {
            // given
            Long memberId = 1L;
            given(walkDiaryRepository.findThreadIdsByMemberIdAndDeletedAtIsNull(memberId)).willReturn(List.of(200L));
            WalkThread thread = createCompletedThread(memberId, 100L);
            given(walkThreadRepository.findAvailableThreadsForDiary(memberId, List.of(200L))).willReturn(List.of(thread));

            // when
            List<AvailableThreadResponse> result = walkDiaryService.getAvailableThreads(memberId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getThreadId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("일기를 작성한 스레드가 없으면 전체 COMPLETED 스레드를 반환한다")
        void availableThreads_noDiariesWritten() {
            // given
            Long memberId = 1L;
            given(walkDiaryRepository.findThreadIdsByMemberIdAndDeletedAtIsNull(memberId)).willReturn(Collections.emptyList());
            WalkThread thread = createCompletedThread(memberId, 100L);
            given(walkThreadRepository.findAvailableThreadsForDiary(memberId, Collections.singletonList(-1L))).willReturn(List.of(thread));

            // when
            List<AvailableThreadResponse> result = walkDiaryService.getAvailableThreads(memberId);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("일기 목록 조회")
    class GetWalkDiaries {

        @Test
        @DisplayName("내 일기 목록은 본인 memberId 기준으로 조회된다")
        void getMyDiaries_success() {
            // given
            WalkDiary diary = WalkDiary.create(1L, null, "내 일기", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 11L);
            PageRequest pageable = PageRequest.of(0, 20);
            Slice<WalkDiary> slice = new SliceImpl<>(List.of(diary), pageable, false);

            given(walkDiaryRepository.findByMemberIdAndDeletedAtIsNull(1L, pageable)).willReturn(slice);

            // when
            SliceResponse<WalkDiaryResponse> response = walkDiaryService.getWalkDiaries(1L, null, pageable);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(11L);
            then(walkDiaryRepository).should().findByMemberIdAndDeletedAtIsNull(1L, pageable);
        }
    }

    @Nested
    @DisplayName("일기 단건 조회")
    class GetDiary {

        @Test
        @DisplayName("비공개 일기를 작성자가 아닌 사용자가 조회하면 예외가 발생한다")
        void getPrivateDiary_byNonOwner_fail() {
            // given
            WalkDiary diary = WalkDiary.create(2L, null, "비공개", "내용", List.of(), LocalDate.now(), false);
            ReflectionTestUtils.setField(diary, "id", 1L);
            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));

            // when & then
            assertThatThrownBy(() -> walkDiaryService.getDiary(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.DIARY_PRIVATE);
        }
    }

    @Nested
    @DisplayName("일기 수정")
    class PatchDiary {

        @Test
        @DisplayName("작성자가 아니면 수정할 수 없다")
        void patch_notOwner_fail() {
            // given
            WalkDiary diary = WalkDiary.create(2L, null, "제목", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 1L);

            WalkDiaryPatchRequest request = new WalkDiaryPatchRequest();
            request.setContent("수정");

            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));

            // when & then
            assertThatThrownBy(() -> walkDiaryService.updateDiary(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.DIARY_OWNER_ONLY);
        }
    }

    @Nested
    @DisplayName("일기 삭제")
    class DeleteDiary {

        @Test
        @DisplayName("작성자가 일기를 삭제하면 soft delete 처리된다")
        void delete_owner_success() {
            // given
            WalkDiary diary = WalkDiary.create(1L, null, "삭제 대상", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 1L);
            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));

            // when
            walkDiaryService.deleteDiary(1L, 1L);

            // then
            assertThat(diary.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("일기 삭제 시 ContentDeletedEvent가 발행된다")
        void delete_publishesContentDeletedEvent() {
            // given
            WalkDiary diary = WalkDiary.create(1L, null, "삭제 대상", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 1L);
            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));

            // when
            walkDiaryService.deleteDiary(1L, 1L);

            // then
            ArgumentCaptor<ContentDeletedEvent> eventCaptor = ArgumentCaptor.forClass(ContentDeletedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());

            ContentDeletedEvent event = eventCaptor.getValue();
            assertThat(event.getMemberId()).isEqualTo(1L);
            assertThat(event.getReferenceId()).isEqualTo(1L);
            assertThat(event.getEventType()).isEqualTo(TimelineEventType.WALK_DIARY_CREATED);
        }
    }

    @Nested
    @DisplayName("팔로잉 일기 피드")
    class FollowingDiaryFeed {

        @Test
        @DisplayName("팔로잉 피드는 공개 일기만 SliceResponse로 반환한다")
        void following_publicOnly_success() {
            // given
            WalkDiary diary = WalkDiary.create(2L, null, "공개", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 10L);

            Slice<WalkDiary> slice = new SliceImpl<>(List.of(diary), PageRequest.of(0, 20), false);
            given(walkDiaryRepository.findFollowingPublicSlice(anyLong(), any())).willReturn(slice);

            // when
            SliceResponse<WalkDiaryResponse> response = walkDiaryService.getFollowingDiaries(1L, PageRequest.of(0, 20));

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(10L);
            assertThat(response.getContent().get(0).isPublic()).isTrue();
        }
    }

    private WalkThread createCompletedThread(Long authorId, Long threadId) {
        return createThreadWithStatus(authorId, threadId, WalkThreadStatus.COMPLETED);
    }

    private WalkThread createThreadWithStatus(Long authorId, Long threadId, WalkThreadStatus status) {
        WalkThread thread = WalkThread.builder()
                .authorId(authorId)
                .title("산책 스레드")
                .description("설명")
                .walkDate(LocalDate.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(BigDecimal.valueOf(37.54))
                .longitude(BigDecimal.valueOf(127.04))
                .address("성동구")
                .status(status)
                .build();
        ReflectionTestUtils.setField(thread, "id", threadId);
        return thread;
    }
}
