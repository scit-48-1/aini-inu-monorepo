package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WalkDiaryServiceTest {

    @Mock
    private WalkDiaryRepository walkDiaryRepository;

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @InjectMocks
    private WalkDiaryService walkDiaryService;

    @Nested
    @DisplayName("일기 생성")
    class CreateDiary {

        @Test
        @DisplayName("공개 범위 미입력 시 기본값은 PUBLIC(true)다")
        void create_defaultPublic_success() {
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
}
