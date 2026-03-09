package scit.ainiinu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.community.dto.StoryDiaryItemResponse;
import scit.ainiinu.community.dto.StoryGroupResponse;
import scit.ainiinu.community.repository.StoryReadRepository;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.dto.response.DiaryThreadSummary;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.service.WalkDiaryService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private static final LocalDateTime DEFAULT_CREATED_AT = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final StoryReadRepository storyReadRepository;
    private final MemberRepository memberRepository;
    private final WalkDiaryService walkDiaryService;

    public SliceResponse<StoryGroupResponse> getStories(Long memberId, Pageable pageable) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        Slice<Long> authorIdSlice = storyReadRepository.findVisibleAuthorIdsForFollower(memberId, cutoff, pageable);

        List<Long> authorIds = authorIdSlice.getContent();
        if (authorIds.isEmpty()) {
            Slice<StoryGroupResponse> emptySlice = new SliceImpl<>(Collections.emptyList(), pageable, false);
            return SliceResponse.of(emptySlice);
        }

        List<WalkDiary> diaries = storyReadRepository.findVisibleDiariesByAuthorIds(authorIds, cutoff);

        Map<Long, DiaryThreadSummary> threadSummaryMap = walkDiaryService.buildThreadSummaryMap(diaries);

        Map<Long, List<WalkDiary>> diariesByAuthorId = diaries.stream()
                .collect(Collectors.groupingBy(WalkDiary::getMemberId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, Member> memberMap = memberRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        List<StoryGroupResponse> groups = new ArrayList<>();
        for (Long authorId : authorIds) {
            List<WalkDiary> authorDiaries = new ArrayList<>(diariesByAuthorId.getOrDefault(authorId, List.of()));
            if (authorDiaries.isEmpty()) {
                continue;
            }

            authorDiaries.sort(Comparator
                    .comparing(this::safeCreatedAt, Comparator.reverseOrder())
                    .thenComparing(WalkDiary::getId, Comparator.nullsLast(Comparator.reverseOrder())));

            Member author = memberMap.get(authorId);
            WalkDiary latestDiary = authorDiaries.get(0);
            List<StoryDiaryItemResponse> diaryItems = authorDiaries.stream()
                    .map(d -> toDiaryItemResponse(d, threadSummaryMap))
                    .toList();

            groups.add(StoryGroupResponse.builder()
                    .memberId(authorId)
                    .nickname(author != null ? author.getNickname() : "이웃")
                    .profileImageUrl(author != null ? author.getProfileImageUrl() : null)
                    .coverImageUrl(resolveCoverImageUrl(latestDiary))
                    .latestCreatedAt(toOffsetDateTime(safeCreatedAt(latestDiary)))
                    .diaries(diaryItems)
                    .build());
        }

        Slice<StoryGroupResponse> mappedSlice = new SliceImpl<>(groups, pageable, authorIdSlice.hasNext());
        return SliceResponse.of(mappedSlice);
    }

    private StoryDiaryItemResponse toDiaryItemResponse(WalkDiary walkDiary, Map<Long, DiaryThreadSummary> threadSummaryMap) {
        DiaryThreadSummary threadSummary = walkDiary.getThreadId() != null
                ? threadSummaryMap.get(walkDiary.getThreadId())
                : null;

        return StoryDiaryItemResponse.builder()
                .diaryId(walkDiary.getId())
                .threadId(walkDiary.getThreadId())
                .title(walkDiary.getTitle())
                .content(walkDiary.getContent())
                .photoUrls(new ArrayList<>(walkDiary.getPhotoUrls()))
                .walkDate(walkDiary.getWalkDate())
                .createdAt(toOffsetDateTime(safeCreatedAt(walkDiary)))
                .thread(threadSummary)
                .build();
    }

    private String resolveCoverImageUrl(WalkDiary walkDiary) {
        List<String> photoUrls = walkDiary.getPhotoUrls();
        if (photoUrls == null || photoUrls.isEmpty()) {
            return null;
        }
        return photoUrls.get(0);
    }

    private LocalDateTime safeCreatedAt(WalkDiary walkDiary) {
        return walkDiary.getCreatedAt() != null ? walkDiary.getCreatedAt() : DEFAULT_CREATED_AT;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
