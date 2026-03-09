package scit.ainiinu.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.ContentDeletedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.AvailableThreadResponse;
import scit.ainiinu.walk.dto.response.DiaryThreadSummary;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.entity.WalkDiary;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.WalkDiaryErrorCode;
import scit.ainiinu.walk.repository.WalkDiaryRepository;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.repository.PetRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkDiaryService {

    private final WalkDiaryRepository walkDiaryRepository;
    private final WalkThreadRepository walkThreadRepository;
    private final WalkThreadApplicationRepository walkThreadApplicationRepository;
    private final WalkThreadPetRepository walkThreadPetRepository;
    private final PetRepository petRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WalkDiaryResponse createDiary(Long memberId, WalkDiaryCreateRequest request) {
        validateThreadForCreate(memberId, request.getThreadId());

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

        String thumbnailUrl = (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty())
                ? request.getPhotoUrls().get(0) : null;
        eventPublisher.publishEvent(ContentCreatedEvent.of(
                memberId, savedDiary.getId(), TimelineEventType.WALK_DIARY_CREATED,
                savedDiary.getTitle(), savedDiary.getContent(), thumbnailUrl
        ));

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

        Map<Long, DiaryThreadSummary> summaryMap = buildThreadSummaryMap(diaries.getContent());
        Slice<WalkDiaryResponse> mapped = diaries.map(d -> toResponse(d, summaryMap));
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

        eventPublisher.publishEvent(ContentDeletedEvent.of(
                memberId, diaryId, TimelineEventType.WALK_DIARY_CREATED
        ));
    }

    public SliceResponse<WalkDiaryResponse> getFollowingDiaries(Long memberId, Pageable pageable) {
        Slice<WalkDiary> slice = walkDiaryRepository.findFollowingPublicSlice(memberId, pageable);
        Map<Long, DiaryThreadSummary> summaryMap = buildThreadSummaryMap(slice.getContent());
        Slice<WalkDiaryResponse> mapped = slice.map(d -> toResponse(d, summaryMap));
        return SliceResponse.of(mapped);
    }

    private WalkDiaryResponse toResponse(WalkDiary walkDiary) {
        String linkedThreadStatus = resolveLinkedThreadStatus(walkDiary.getThreadId());
        DiaryThreadSummary summary = buildSingleThreadSummary(walkDiary.getThreadId());
        return WalkDiaryResponse.from(walkDiary, linkedThreadStatus, summary);
    }

    private WalkDiaryResponse toResponse(WalkDiary walkDiary, Map<Long, DiaryThreadSummary> summaryMap) {
        String linkedThreadStatus = resolveLinkedThreadStatus(walkDiary.getThreadId());
        DiaryThreadSummary summary = walkDiary.getThreadId() != null ? summaryMap.get(walkDiary.getThreadId()) : null;
        return WalkDiaryResponse.from(walkDiary, linkedThreadStatus, summary);
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

    private DiaryThreadSummary buildSingleThreadSummary(Long threadId) {
        if (threadId == null) {
            return null;
        }
        Map<Long, DiaryThreadSummary> map = buildThreadSummaryMapForIds(List.of(threadId));
        return map.get(threadId);
    }

    public Map<Long, DiaryThreadSummary> buildThreadSummaryMap(List<WalkDiary> diaries) {
        List<Long> threadIds = diaries.stream()
                .map(WalkDiary::getThreadId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (threadIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return buildThreadSummaryMapForIds(threadIds);
    }

    private Map<Long, DiaryThreadSummary> buildThreadSummaryMapForIds(List<Long> threadIds) {
        Map<Long, WalkThread> threadMap = walkThreadRepository.findAllById(threadIds).stream()
                .filter(t -> t.getStatus() != WalkThreadStatus.DELETED)
                .collect(Collectors.toMap(WalkThread::getId, Function.identity()));

        if (threadMap.isEmpty()) {
            return Collections.emptyMap();
        }

        List<WalkThreadPet> threadPets = walkThreadPetRepository.findAllByThreadIdIn(threadMap.keySet().stream().toList());

        List<Long> petIds = threadPets.stream()
                .map(WalkThreadPet::getPetId)
                .distinct()
                .toList();

        Map<Long, Pet> petMap = petIds.isEmpty()
                ? Collections.emptyMap()
                : petRepository.findAllById(petIds).stream()
                        .collect(Collectors.toMap(Pet::getId, Function.identity()));

        Map<Long, List<WalkThreadPet>> petsByThread = threadPets.stream()
                .collect(Collectors.groupingBy(WalkThreadPet::getThreadId));

        return threadMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    WalkThread thread = entry.getValue();
                    List<DiaryThreadSummary.PetCard> petCards = petsByThread
                            .getOrDefault(thread.getId(), List.of()).stream()
                            .map(tp -> petMap.get(tp.getPetId()))
                            .filter(Objects::nonNull)
                            .map(pet -> DiaryThreadSummary.PetCard.builder()
                                    .id(pet.getId())
                                    .name(pet.getName())
                                    .photoUrl(pet.getPhotoUrl())
                                    .breedName(pet.getBreed() != null ? pet.getBreed().getName() : null)
                                    .build())
                            .toList();

                    return DiaryThreadSummary.builder()
                            .threadId(thread.getId())
                            .walkDate(thread.getWalkDate())
                            .startTime(thread.getStartTime())
                            .endTime(thread.getEndTime())
                            .placeName(thread.getPlaceName())
                            .latitude(thread.getLatitude())
                            .longitude(thread.getLongitude())
                            .address(thread.getAddress())
                            .pets(petCards)
                            .build();
                }));
    }

    public List<AvailableThreadResponse> getAvailableThreads(Long memberId) {
        List<Long> alreadyWrittenThreadIds = walkDiaryRepository.findThreadIdsByMemberIdAndDeletedAtIsNull(memberId);
        List<Long> excludeIds = alreadyWrittenThreadIds.isEmpty() ? Collections.singletonList(-1L) : alreadyWrittenThreadIds;

        List<WalkThread> threads = walkThreadRepository.findAvailableThreadsForDiary(memberId, excludeIds);
        return threads.stream()
                .map(AvailableThreadResponse::from)
                .toList();
    }

    private void validateThreadForCreate(Long memberId, Long threadId) {
        WalkThread thread = walkThreadRepository.findById(threadId)
                .orElseThrow(() -> new BusinessException(WalkDiaryErrorCode.THREAD_NOT_FOUND));

        if (thread.getStatus() != WalkThreadStatus.COMPLETED) {
            throw new BusinessException(WalkDiaryErrorCode.THREAD_NOT_COMPLETED);
        }

        boolean isParticipant = thread.isAuthor(memberId)
                || walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(
                        threadId, memberId, WalkThreadApplicationStatus.JOINED).isPresent();
        if (!isParticipant) {
            throw new BusinessException(WalkDiaryErrorCode.NOT_THREAD_PARTICIPANT);
        }

        if (walkDiaryRepository.existsByMemberIdAndThreadIdAndDeletedAtIsNull(memberId, threadId)) {
            throw new BusinessException(WalkDiaryErrorCode.DIARY_ALREADY_EXISTS);
        }
    }

    private void validateThreadId(Long threadId) {
        if (threadId == null) {
            return;
        }

        walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED)
                .orElseThrow(() -> new BusinessException(WalkDiaryErrorCode.THREAD_NOT_FOUND));
    }
}
