package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WalkDiaryServiceFollowingTest {

    @Mock
    private WalkDiaryRepository walkDiaryRepository;

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @InjectMocks
    private WalkDiaryService walkDiaryService;

    @Test
    @DisplayName("팔로잉 피드는 page,size,hasNext를 유지한 SliceResponse로 반환한다")
    void getFollowingDiaries_pagination_success() {
        // given
        WalkDiary diary1 = WalkDiary.create(2L, null, "일기1", "내용", List.of(), LocalDate.now(), true);
        WalkDiary diary2 = WalkDiary.create(3L, null, "일기2", "내용", List.of(), LocalDate.now(), true);
        ReflectionTestUtils.setField(diary1, "id", 11L);
        ReflectionTestUtils.setField(diary2, "id", 12L);

        Slice<WalkDiary> slice = new SliceImpl<>(List.of(diary1, diary2), PageRequest.of(0, 2), true);
        given(walkDiaryRepository.findFollowingPublicSlice(eq(1L), any())).willReturn(slice);

        // when
        SliceResponse<WalkDiaryResponse> response = walkDiaryService.getFollowingDiaries(1L, PageRequest.of(0, 2));

        // then
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();
    }
}
