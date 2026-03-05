package scit.ainiinu.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkDiaryService {

    private final WalkDiaryRepository walkDiaryRepository;
    private final WalkThreadRepository walkThreadRepository;

    @Transactional
    public WalkDiaryResponse createDiary(Long memberId, WalkDiaryCreateRequest request) {
        validateThreadId(request.getThreadId());

        WalkDiary walkDiary = WalkDiary.create(
                memberId,
                request.getThreadId(),
                request.getTitle(),
                request.getContent(),
                request.getPhotoUrls(),
                request.getWalkDate(),
                request.resolveIsPublic()
        );

        WalkDiary savedDiary = walkDiaryRepository.save(walkDiary);
        return toResponse(savedDiary);
    }

    public SliceResponse<WalkDiaryResponse> getWalkDiaries(Long memberId, Long targetMemberId, Pageable pageable) {
        Long resolvedTargetMemberId = targetMemberId != null ? targetMemberId : memberId;

        Slice<WalkDiary> diaries;
        if (resolvedTargetMemberId.equals(memberId)) {
            diaries = walkDiaryRepository.findByMemberIdAndDeletedAtIsNull(resolvedTargetMemberId, pageable);
        } else {
            diaries = walkDiaryRepository.findByMemberIdAndIsPublicTrueAndDeletedAtIsNull(resolvedTargetMemberId, pageable);
        }

        Slice<WalkDiaryResponse> mapped = diaries.map(this::toResponse);
        return SliceResponse.of(mapped);
    }

    public WalkDiaryResponse getDiary(Long memberId, Long diaryId) {
        WalkDiary walkDiary = walkDiaryRepository.findByIdAndDeletedAtIsNull(diaryId)
                .orElseThrow(() -> new BusinessException(WalkDiaryErrorCode.DIARY_NOT_FOUND));

        if (!walkDiary.isOwner(memberId) && !Boolean.TRUE.equals(walkDiary.getIsPublic())) {
            throw new BusinessException(WalkDiaryErrorCode.DIARY_PRIVATE);
        }

        return toResponse(walkDiary);
    }

    @Transactional
    public WalkDiaryResponse updateDiary(Long memberId, Long diaryId, WalkDiaryPatchRequest request) {
        WalkDiary walkDiary = walkDiaryRepository.findByIdAndDeletedAtIsNull(diaryId)
                .orElseThrow(() -> new BusinessException(WalkDiaryErrorCode.DIARY_NOT_FOUND));

        if (!walkDiary.isOwner(memberId)) {
            throw new BusinessException(WalkDiaryErrorCode.DIARY_OWNER_ONLY);
        }

        validateThreadId(request.getThreadId());

        walkDiary.update(
                request.getThreadId(),
                request.getTitle(),
                request.getContent(),
                request.getPhotoUrls(),
                request.getWalkDate(),
                request.getIsPublic()
        );

        return toResponse(walkDiary);
    }

    @Transactional
    public void deleteDiary(Long memberId, Long diaryId) {
        WalkDiary walkDiary = walkDiaryRepository.findByIdAndDeletedAtIsNull(diaryId)
                .orElseThrow(() -> new BusinessException(WalkDiaryErrorCode.DIARY_NOT_FOUND));

        if (!walkDiary.isOwner(memberId)) {
            throw new BusinessException(WalkDiaryErrorCode.DIARY_OWNER_ONLY);
        }

        walkDiary.softDelete(LocalDateTime.now());
    }

    public SliceResponse<WalkDiaryResponse> getFollowingDiaries(Long memberId, Pageable pageable) {
        Slice<WalkDiary> slice = walkDiaryRepository.findFollowingPublicSlice(memberId, pageable);
        Slice<WalkDiaryResponse> mapped = slice.map(this::toResponse);
        return SliceResponse.of(mapped);
    }

    private WalkDiaryResponse toResponse(WalkDiary walkDiary) {
        String linkedThreadStatus = resolveLinkedThreadStatus(walkDiary.getThreadId());
        return WalkDiaryResponse.from(walkDiary, linkedThreadStatus);
    }

    private String resolveLinkedThreadStatus(Long threadId) {
        if (threadId == null) {
            return "NONE";
        }

        Optional<WalkThread> walkThread = walkThreadRepository.findById(threadId);
        if (walkThread.isEmpty()) {
            return "DELETED";
        }

        if (walkThread.get().getStatus() == WalkThreadStatus.DELETED) {
            return "DELETED";
        }

        return "ACTIVE";
    }

    private void validateThreadId(Long threadId) {
        if (threadId == null) {
            return;
        }

        walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WalkDiaryErrorCode.THREAD_NOT_FOUND));
    }
}
