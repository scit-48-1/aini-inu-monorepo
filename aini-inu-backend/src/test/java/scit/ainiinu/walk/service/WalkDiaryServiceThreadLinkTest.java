package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
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

@ExtendWith(MockitoExtension.class)
class WalkDiaryServiceThreadLinkTest {

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

    @Test
    @DisplayName("threadId가 없으면 linkedThreadStatus는 NONE이다")
    void linkedThreadStatus_none_success() {
        // given
        WalkDiary diary = WalkDiary.create(1L, null, "일기", "내용", List.of(), LocalDate.now(), true);
        ReflectionTestUtils.setField(diary, "id", 10L);
        given(walkDiaryRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(diary));

        // when
        WalkDiaryResponse response = walkDiaryService.getDiary(1L, 10L);

        // then
        assertThat(response.getLinkedThreadStatus()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("연결된 스레드가 활성 상태면 linkedThreadStatus는 ACTIVE다")
    void linkedThreadStatus_active_success() {
        // given
        WalkDiary diary = WalkDiary.create(1L, 100L, "일기", "내용", List.of(), LocalDate.now(), true);
        ReflectionTestUtils.setField(diary, "id", 11L);

        WalkThread thread = createThread(100L, WalkThreadStatus.RECRUITING);

        given(walkDiaryRepository.findByIdAndDeletedAtIsNull(11L)).willReturn(Optional.of(diary));
        given(walkThreadRepository.findById(100L)).willReturn(Optional.of(thread));

        // when
        WalkDiaryResponse response = walkDiaryService.getDiary(1L, 11L);

        // then
        assertThat(response.getLinkedThreadStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("연결된 스레드가 삭제되면 linkedThreadStatus는 DELETED다")
    void linkedThreadStatus_deleted_success() {
        // given
        WalkDiary diary = WalkDiary.create(1L, 100L, "일기", "내용", List.of(), LocalDate.now(), true);
        ReflectionTestUtils.setField(diary, "id", 12L);

        WalkThread thread = createThread(100L, WalkThreadStatus.DELETED);

        given(walkDiaryRepository.findByIdAndDeletedAtIsNull(12L)).willReturn(Optional.of(diary));
        given(walkThreadRepository.findById(100L)).willReturn(Optional.of(thread));

        // when
        WalkDiaryResponse response = walkDiaryService.getDiary(1L, 12L);

        // then
        assertThat(response.getLinkedThreadStatus()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("생성 시 존재하지 않는 threadId는 거절한다")
    void createDiary_invalidThreadId_fail() {
        // given
        WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
        request.setThreadId(999L);
        request.setTitle("스레드 연결 일기");
        request.setContent("본문");
        request.setPhotoUrls(List.of());
        request.setWalkDate(LocalDate.now());
        request.setIsPublic(true);

        given(walkThreadRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> walkDiaryService.createDiary(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.THREAD_NOT_FOUND);
    }

    private WalkThread createThread(Long id, WalkThreadStatus status) {
        WalkThread thread = WalkThread.builder()
                .authorId(1L)
                .title("테스트 스레드")
                .description("설명")
                .walkDate(LocalDate.now().plusDays(1))
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
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
        ReflectionTestUtils.setField(thread, "id", id);
        return thread;
    }
}
