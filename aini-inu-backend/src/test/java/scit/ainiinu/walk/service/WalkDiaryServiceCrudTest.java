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
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WalkDiaryServiceCrudTest {

    @Mock
    private WalkDiaryRepository walkDiaryRepository;

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalkDiaryService walkDiaryService;

    @Nested
    @DisplayName("일기 생성")
    class CreateDiary {

        @Test
        @DisplayName("공개 범위 미입력 시 기본값은 PUBLIC(true)다")
        void createDiary_defaultVisibilityPublic_success() {
            // given
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setTitle("한강 산책 일기");
            request.setContent("오늘 날씨가 좋았다");
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("https://cdn/1.jpg"));
            request.setIsPublic(null);

            given(walkDiaryRepository.save(any(WalkDiary.class))).willAnswer(invocation -> {
                WalkDiary diary = invocation.getArgument(0);
                ReflectionTestUtils.setField(diary, "id", 1L);
                return diary;
            });

            // when
            WalkDiaryResponse response = walkDiaryService.createDiary(1L, request);

            // then
            assertThat(response.isPublic()).isTrue();
            ArgumentCaptor<WalkDiary> captor = ArgumentCaptor.forClass(WalkDiary.class);
            then(walkDiaryRepository).should().save(captor.capture());
            assertThat(captor.getValue().getIsPublic()).isTrue();
        }

        @Test
        @DisplayName("사진은 최대 5장까지만 허용한다")
        void createDiary_photoMaxFive_fail() {
            // given
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setTitle("한강 산책 일기");
            request.setContent("본문");
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of("1", "2", "3", "4", "5", "6"));
            request.setIsPublic(true);

            // when & then
            assertThatThrownBy(() -> walkDiaryService.createDiary(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.IMAGE_COUNT_EXCEEDED);
        }

        @Test
        @DisplayName("본문이 300자를 초과하면 생성할 수 없다")
        void createDiary_contentTooLong_fail() {
            WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
            request.setTitle("한강 산책 일기");
            request.setContent("a".repeat(301));
            request.setWalkDate(LocalDate.now());
            request.setPhotoUrls(List.of());
            request.setIsPublic(true);

            assertThatThrownBy(() -> walkDiaryService.createDiary(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("일기 수정/삭제")
    class UpdateDeleteDiary {

        @Test
        @DisplayName("작성자가 아니면 수정할 수 없다")
        void updateDiary_notOwner_fail() {
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

        @Test
        @DisplayName("작성자가 아니면 삭제할 수 없다")
        void deleteDiary_notOwner_fail() {
            // given
            WalkDiary diary = WalkDiary.create(2L, null, "제목", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 1L);

            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));

            // when & then
            assertThatThrownBy(() -> walkDiaryService.deleteDiary(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.DIARY_OWNER_ONLY);
        }

        @Test
        @DisplayName("threadId가 유효하지 않으면 수정할 수 없다")
        void updateDiary_invalidThreadId_fail() {
            // given
            WalkDiary diary = WalkDiary.create(1L, null, "제목", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 1L);

            WalkDiaryPatchRequest request = new WalkDiaryPatchRequest();
            request.setThreadId(333L);

            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));
            given(walkThreadRepository.findByIdAndStatusNot(eq(333L), any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> walkDiaryService.updateDiary(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.THREAD_NOT_FOUND);
        }

        @Test
        @DisplayName("본문이 300자를 초과하면 수정할 수 없다")
        void updateDiary_contentTooLong_fail() {
            WalkDiary diary = WalkDiary.create(1L, null, "제목", "내용", List.of(), LocalDate.now(), true);
            ReflectionTestUtils.setField(diary, "id", 1L);

            WalkDiaryPatchRequest request = new WalkDiaryPatchRequest();
            request.setContent("a".repeat(301));

            given(walkDiaryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(diary));

            assertThatThrownBy(() -> walkDiaryService.updateDiary(1L, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.INVALID_REQUEST);
        }
    }

    private WalkThread createActiveThread() {
        WalkThread thread = WalkThread.builder()
                .authorId(1L)
                .title("활성 스레드")
                .description("설명")
                .walkDate(LocalDate.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(1))
                .chatType(scit.ainiinu.walk.entity.WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(BigDecimal.valueOf(37.54))
                .longitude(BigDecimal.valueOf(127.04))
                .address("성동구")
                .status(scit.ainiinu.walk.entity.WalkThreadStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(thread, "id", 100L);
        return thread;
    }
}
