package scit.ainiinu.community.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import scit.ainiinu.community.dto.StoryDiaryItemResponse;
import scit.ainiinu.community.dto.StoryGroupResponse;
import scit.ainiinu.community.repository.StoryReadRepository;
import scit.ainiinu.community.service.StoryService;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryReadRepository storyReadRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private StoryService storyService;

    @Nested
    @DisplayName("스토리 목록 조회")
    class GetStories {

        @Test
        @DisplayName("회원 그룹 단위로 스토리를 반환하고 그룹 내부 산책일기는 최신순으로 정렬한다")
        void returnsGroupedStoriesWithSortedDiaries() {
            Long memberId = 1L;
            Long authorAId = 10L;
            Long authorBId = 20L;

            WalkDiary authorAOlderDiary = createDiary(
                    100L,
                    authorAId,
                    "A 오래된 일기",
                    List.of("https://cdn.example.com/a-older.jpg"),
                    LocalDateTime.now().minusHours(2)
            );
            WalkDiary authorANewDiary = createDiary(
                    101L,
                    authorAId,
                    "A 최신 일기",
                    List.of("https://cdn.example.com/a-new.jpg"),
                    LocalDateTime.now().minusMinutes(10)
            );
            WalkDiary authorBDiary = createDiary(
                    102L,
                    authorBId,
                    "B 일기",
                    List.of("https://cdn.example.com/b.jpg"),
                    LocalDateTime.now().minusHours(1)
            );

            Slice<Long> authorIdSlice = new SliceImpl<>(List.of(authorAId, authorBId), PageRequest.of(0, 20), false);
            given(storyReadRepository.findVisibleAuthorIdsForFollower(eq(memberId), any(LocalDateTime.class), any()))
                    .willReturn(authorIdSlice);
            given(storyReadRepository.findVisibleDiariesByAuthorIds(eq(List.of(authorAId, authorBId)), any(LocalDateTime.class)))
                    .willReturn(List.of(authorAOlderDiary, authorBDiary, authorANewDiary));

            Member authorA = Member.builder()
                    .email("walker@example.com")
                    .nickname("몽이아빠")
                    .profileImageUrl("https://cdn.example.com/profile.jpg")
                    .memberType(MemberType.PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(authorA, "id", authorAId);

            Member authorB = Member.builder()
                    .email("runner@example.com")
                    .nickname("보리누나")
                    .profileImageUrl("https://cdn.example.com/profile2.jpg")
                    .memberType(MemberType.PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(authorB, "id", authorBId);

            given(memberRepository.findAllById(List.of(authorAId, authorBId))).willReturn(List.of(authorA, authorB));

            SliceResponse<StoryGroupResponse> response = storyService.getStories(memberId, PageRequest.of(0, 20));

            assertThat(response.getContent()).hasSize(2);

            StoryGroupResponse groupA = response.getContent().get(0);
            assertThat(groupA.getMemberId()).isEqualTo(authorAId);
            assertThat(groupA.getNickname()).isEqualTo("몽이아빠");
            assertThat(groupA.getCoverImageUrl()).isEqualTo("https://cdn.example.com/a-new.jpg");
            assertThat(groupA.getDiaries()).hasSize(2);
            assertThat(groupA.getDiaries()).extracting(StoryDiaryItemResponse::getDiaryId).containsExactly(101L, 100L);

            StoryGroupResponse groupB = response.getContent().get(1);
            assertThat(groupB.getMemberId()).isEqualTo(authorBId);
            assertThat(groupB.getNickname()).isEqualTo("보리누나");
            assertThat(groupB.getDiaries()).hasSize(1);
            assertThat(groupB.getDiaries().get(0).getDiaryId()).isEqualTo(102L);
        }

        @Test
        @DisplayName("조회 가능한 스토리 작성자가 없으면 빈 Slice를 반환한다")
        void returnsEmptySliceWhenNoVisibleAuthor() {
            Long memberId = 1L;
            given(storyReadRepository.findVisibleAuthorIdsForFollower(eq(memberId), any(LocalDateTime.class), any()))
                    .willReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

            SliceResponse<StoryGroupResponse> response = storyService.getStories(memberId, PageRequest.of(0, 20));

            assertThat(response.getContent()).isEmpty();
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("회원 프로필이 없으면 닉네임 fallback(이웃)을 사용한다")
        void usesFallbackNicknameWhenMemberNotFound() {
            Long memberId = 1L;
            Long authorId = 10L;

            WalkDiary diary = createDiary(
                    200L,
                    authorId,
                    "일기",
                    List.of("https://cdn.example.com/image.jpg"),
                    LocalDateTime.now().minusMinutes(30)
            );

            given(storyReadRepository.findVisibleAuthorIdsForFollower(eq(memberId), any(LocalDateTime.class), any()))
                    .willReturn(new SliceImpl<>(List.of(authorId), PageRequest.of(0, 20), false));
            given(storyReadRepository.findVisibleDiariesByAuthorIds(eq(List.of(authorId)), any(LocalDateTime.class)))
                    .willReturn(List.of(diary));
            given(memberRepository.findAllById(List.of(authorId))).willReturn(List.of());

            SliceResponse<StoryGroupResponse> response = storyService.getStories(memberId, PageRequest.of(0, 20));

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getNickname()).isEqualTo("이웃");
        }
    }

    private WalkDiary createDiary(Long diaryId, Long authorId, String title, List<String> photos, LocalDateTime createdAt) {
        WalkDiary diary = WalkDiary.create(
                authorId,
                null,
                title,
                "내용",
                photos,
                LocalDate.now(),
                true
        );
        ReflectionTestUtils.setField(diary, "id", diaryId);
        ReflectionTestUtils.setField(diary, "createdAt", createdAt);
        return diary;
    }
}
