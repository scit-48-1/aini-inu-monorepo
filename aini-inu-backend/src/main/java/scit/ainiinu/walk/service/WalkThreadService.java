package scit.ainiinu.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.ContentDeletedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.walk.dto.request.ThreadApplyRequest;
import scit.ainiinu.walk.dto.request.ThreadCreateRequest;
import scit.ainiinu.walk.dto.request.ThreadPatchRequest;
import scit.ainiinu.walk.dto.response.ThreadApplyResponse;
import scit.ainiinu.walk.dto.response.ThreadHotspotResponse;
import scit.ainiinu.walk.dto.response.ThreadMapResponse;
import scit.ainiinu.walk.dto.response.ThreadResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.walk.entity.WalkThreadFilter;
import scit.ainiinu.walk.entity.WalkThreadApplicationPet;
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.ThreadErrorCode;
import scit.ainiinu.walk.repository.WalkThreadApplicationPetRepository;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadFilterRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkThreadService {

    private final WalkThreadRepository walkThreadRepository;
    private final WalkThreadPetRepository walkThreadPetRepository;
    private final WalkThreadFilterRepository walkThreadFilterRepository;
    private final WalkThreadApplicationRepository walkThreadApplicationRepository;
    private final WalkThreadApplicationPetRepository walkThreadApplicationPetRepository;
    private final MemberRepository memberRepository;
    private final PetRepository petRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ThreadResponse createThread(Long memberId, ThreadCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ThreadErrorCode.NON_PET_OWNER_CREATE_FORBIDDEN));
        if (member.getMemberType() == MemberType.NON_PET_OWNER) {
            throw new BusinessException(ThreadErrorCode.NON_PET_OWNER_CREATE_FORBIDDEN);
        }

        validateActiveThreadLimit(memberId);

        WalkChatType chatType = resolveChatType(request.getChatType());

        WalkThread thread = WalkThread.builder()
                .authorId(memberId)
                .title(request.getTitle())
                .description(request.getDescription())
                .walkDate(request.getWalkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .chatType(chatType)
                .maxParticipants(request.getMaxParticipants())
                .allowNonPetOwner(request.getAllowNonPetOwner())
                .isVisibleAlways(request.getIsVisibleAlways())
                .placeName(request.getLocation().getPlaceName())
                .latitude(toDecimal(request.getLocation().getLatitude(), 6))
                .longitude(toDecimal(request.getLocation().getLongitude(), 6))
                .address(request.getLocation().getAddress())
                .status(WalkThreadStatus.RECRUITING)
                .build();

        WalkThread savedThread = walkThreadRepository.save(thread);
        saveThreadPets(savedThread.getId(), request.getPetIds());
        saveThreadFilters(savedThread.getId(), request.getFilters());

        eventPublisher.publishEvent(ContentCreatedEvent.of(
                memberId, savedThread.getId(), TimelineEventType.WALK_THREAD_CREATED,
                savedThread.getTitle(), savedThread.getPlaceName(), null));

        return toThreadResponse(savedThread, memberId);
    }

    public SliceResponse<ThreadSummaryResponse> getThreads(Long memberId, Pageable pageable, LocalDate startDate, LocalDate endDate, Double latitude, Double longitude, Double radiusKm) {
        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> allRecruiting = walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING);

        final boolean hasLocation = latitude != null && longitude != null && radiusKm != null;
        final double lat = hasLocation ? latitude : 0;
        final double lng = hasLocation ? longitude : 0;
        final double rad = hasLocation ? radiusKm : 0;

        List<WalkThread> filteredThreads = new ArrayList<>();
        for (WalkThread thread : allRecruiting) {
            if (thread.isExpired(now)) {
                continue;
            }
            if (startDate != null && thread.getWalkDate().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && thread.getWalkDate().isAfter(endDate)) {
                continue;
            }
            if (hasLocation && distanceInKm(lat, lng,
                    thread.getLatitude().doubleValue(), thread.getLongitude().doubleValue()) > rad) {
                continue;
            }
            filteredThreads.add(thread);
        }

        filteredThreads.sort(Comparator.comparing(WalkThread::getCreatedAt).reversed()
                .thenComparing(Comparator.comparing(WalkThread::getId).reversed()));

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, filteredThreads.size());
        boolean hasNext = end < filteredThreads.size();

        List<WalkThread> pageContent = start < filteredThreads.size()
                ? filteredThreads.subList(start, end)
                : List.of();

        List<ThreadSummaryResponse> content = toBatchSummaryResponses(pageContent, memberId);
        Pageable safePageable = PageRequest.of(pageNumber, pageSize);
        Slice<ThreadSummaryResponse> slice = new org.springframework.data.domain.SliceImpl<>(
                content, safePageable, hasNext);
        return SliceResponse.of(slice);
    }

    public List<ThreadMapResponse> getMapThreads(Long memberId, double latitude, double longitude, double radiusKm, LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> recruitingThreads = walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING);

        List<WalkThread> filteredThreads = new ArrayList<>();
        for (WalkThread thread : recruitingThreads) {
            if (thread.isExpired(now)) {
                continue;
            }
            if (startDate != null && thread.getWalkDate().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && thread.getWalkDate().isAfter(endDate)) {
                continue;
            }
            double distance = distanceInKm(latitude, longitude, thread.getLatitude().doubleValue(), thread.getLongitude().doubleValue());
            if (distance <= radiusKm) {
                filteredThreads.add(thread);
            }
        }

        List<Long> threadIds = filteredThreads.stream().map(WalkThread::getId).toList();
        Map<Long, Long> countMap = batchCountByStatus(threadIds, WalkThreadApplicationStatus.JOINED);

        // Batch fetch first pet image for each thread (2 queries, no N+1)
        List<WalkThreadPet> allThreadPets = walkThreadPetRepository.findAllByThreadIdIn(threadIds);
        Map<Long, Long> threadFirstPetMap = new HashMap<>();
        for (WalkThreadPet tp : allThreadPets) {
            threadFirstPetMap.putIfAbsent(tp.getThreadId(), tp.getPetId());
        }
        List<Long> petIds = new ArrayList<>(new HashSet<>(threadFirstPetMap.values()));
        Map<Long, String> petPhotoMap = new HashMap<>();
        if (!petIds.isEmpty()) {
            for (Pet pet : petRepository.findAllById(petIds)) {
                if (pet.getPhotoUrl() != null) {
                    petPhotoMap.put(pet.getId(), pet.getPhotoUrl());
                }
            }
        }

        List<ThreadMapResponse> results = new ArrayList<>();
        for (WalkThread thread : filteredThreads) {
            long currentParticipants = countMap.getOrDefault(thread.getId(), 0L) + 1;
            Long firstPetId = threadFirstPetMap.get(thread.getId());
            String petImageUrl = firstPetId != null ? petPhotoMap.get(firstPetId) : null;
            results.add(ThreadMapResponse.builder()
                    .threadId(thread.getId())
                    .title(thread.getTitle())
                    .chatType(thread.getChatType().name())
                    .currentParticipants((int) currentParticipants)
                    .maxParticipants(thread.getMaxParticipants())
                    .latitude(thread.getLatitude())
                    .longitude(thread.getLongitude())
                    .placeName(thread.getPlaceName())
                    .petImageUrl(petImageUrl)
                    .build());
        }

        results.sort(Comparator.comparing(ThreadMapResponse::getThreadId).reversed());
        return results;
    }

    public ThreadResponse getThread(Long memberId, Long threadId) {
        WalkThread thread = walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ThreadErrorCode.THREAD_NOT_FOUND));
        return toThreadResponse(thread, memberId);
    }

    @Transactional
    public ThreadResponse updateThread(Long memberId, Long threadId, ThreadPatchRequest request) {
        WalkThread thread = walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ThreadErrorCode.THREAD_NOT_FOUND));

        if (!thread.isAuthor(memberId)) {
            throw new BusinessException(ThreadErrorCode.THREAD_OWNER_ONLY);
        }

        WalkChatType chatType = request.getChatType() != null ? resolveChatType(request.getChatType()) : null;
        ThreadCreateRequest.LocationRequest location = request.getLocation();

        thread.update(
                request.getTitle(),
                request.getDescription(),
                request.getWalkDate(),
                request.getStartTime(),
                request.getEndTime(),
                chatType,
                request.getMaxParticipants(),
                location != null ? location.getPlaceName() : null,
                location != null && location.getLatitude() != null ? toDecimal(location.getLatitude(), 6) : null,
                location != null && location.getLongitude() != null ? toDecimal(location.getLongitude(), 6) : null,
                location != null ? location.getAddress() : null,
                request.getAllowNonPetOwner(),
                request.getIsVisibleAlways()
        );

        // Must flush before pet delete: deleteAllByThreadId uses @Modifying(clearAutomatically = true)
        // which clears the persistence context. Hibernate AUTO-flush only flushes entities
        // overlapping the query's table (WalkThreadPet), not WalkThread — so dirty field
        // changes (title, date, chatType, etc.) would be lost without an explicit flush here.
        thread = walkThreadRepository.saveAndFlush(thread);

        if (request.getPetIds() != null) {
            walkThreadPetRepository.deleteAllByThreadId(threadId);
            walkThreadPetRepository.flush();
            saveThreadPets(threadId, request.getPetIds());
        }

        if (request.getFilters() != null) {
            walkThreadFilterRepository.deleteAllByThreadId(threadId);
            saveThreadFilters(threadId, request.getFilters());
        }

        return toThreadResponse(thread, memberId);
    }

    @Transactional
    public void deleteThread(Long memberId, Long threadId) {
        WalkThread thread = walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ThreadErrorCode.THREAD_NOT_FOUND));

        if (!thread.isAuthor(memberId)) {
            throw new BusinessException(ThreadErrorCode.THREAD_OWNER_ONLY);
        }

        thread.markDeleted();

        eventPublisher.publishEvent(ContentDeletedEvent.of(
                memberId, threadId, TimelineEventType.WALK_THREAD_CREATED));
    }

    @Transactional
    public ThreadApplyResponse applyThread(Long memberId, Long threadId, ThreadApplyRequest request) {
        WalkThread thread = walkThreadRepository.findByIdAndStatusNot(threadId, WalkThreadStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ThreadErrorCode.THREAD_NOT_FOUND));

        if (thread.isExpired(LocalDateTime.now())) {
            thread.expire();
            throw new BusinessException(ThreadErrorCode.THREAD_EXPIRED);
        }

        if (thread.isAuthor(memberId)) {
            throw new BusinessException(ThreadErrorCode.APPLY_FORBIDDEN_SELF);
        }

        Optional<WalkThreadApplication> existing = walkThreadApplicationRepository.findByThreadIdAndMemberId(threadId, memberId);
        if (existing.isPresent() && existing.get().isJoined()) {
            return ThreadApplyResponse.builder()
                    .threadId(threadId)
                    .chatRoomId(existing.get().getChatRoomId())
                    .applicationStatus(WalkThreadApplicationStatus.JOINED.name())
                    .isIdempotentReplay(true)
                    .build();
        }

        long joinedCount = walkThreadApplicationRepository.countByThreadIdAndStatus(
                threadId,
                WalkThreadApplicationStatus.JOINED
        );
        if (joinedCount + 1 >= thread.getMaxParticipants()) {
            throw new BusinessException(ThreadErrorCode.CAPACITY_FULL);
        }

        Long chatRoomId = resolveChatRoomId(thread, memberId);
        if (existing.isPresent()) {
            existing.get().rejoin(chatRoomId);
            walkThreadApplicationPetRepository.deleteAllByApplicationId(existing.get().getId());
            saveApplicationPets(existing.get().getId(), request.getPetIds());
        } else {
            WalkThreadApplication application = WalkThreadApplication.joined(threadId, memberId, chatRoomId);
            WalkThreadApplication savedApplication = walkThreadApplicationRepository.save(application);
            saveApplicationPets(savedApplication.getId(), request.getPetIds());
        }

        return ThreadApplyResponse.builder()
                .threadId(threadId)
                .chatRoomId(chatRoomId)
                .applicationStatus(WalkThreadApplicationStatus.JOINED.name())
                .isIdempotentReplay(false)
                .build();
    }

    @Transactional
    public void cancelApplyThread(Long memberId, Long threadId) {
        WalkThreadApplication application = walkThreadApplicationRepository
                .findByThreadIdAndMemberIdAndStatus(threadId, memberId, WalkThreadApplicationStatus.JOINED)
                .orElseThrow(() -> new BusinessException(ThreadErrorCode.THREAD_APPLY_NOT_FOUND));

        application.cancel();
        walkThreadApplicationRepository.flush();
        walkThreadApplicationPetRepository.deleteAllByApplicationId(application.getId());
    }

    public List<ThreadHotspotResponse> getHotspots(int hours) {
        int safeHours = Math.max(hours, 1);
        LocalDateTime createdAfter = LocalDateTime.now().minusHours(safeHours);
        List<WalkThread> threads = walkThreadRepository.findByStatusAndCreatedAfter(WalkThreadStatus.RECRUITING, createdAfter);

        Map<String, Long> countByRegion = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (WalkThread thread : threads) {
            if (thread.isExpired(now)) {
                continue;
            }
            if ("현재위치".equals(thread.getPlaceName())) {
                continue;
            }
            countByRegion.merge(thread.getPlaceName(), 1L, Long::sum);
        }

        List<ThreadHotspotResponse> responses = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByRegion.entrySet()) {
            responses.add(new ThreadHotspotResponse(entry.getKey(), entry.getValue()));
        }
        responses.sort(Comparator.comparing(ThreadHotspotResponse::getCount).reversed());
        if (responses.size() > 5) {
            return responses.subList(0, 5);
        }
        return responses;
    }

    public List<ThreadSummaryResponse> getMyActiveThread(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> threads = walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING);
        List<WalkThread> activeThreads = threads.stream()
                .filter(thread -> !thread.isExpired(now))
                .toList();
        return toBatchSummaryResponses(activeThreads, memberId);
    }

    public List<ThreadSummaryResponse> getMyJoinedThreads(Long memberId) {
        List<WalkThreadApplication> applications = walkThreadApplicationRepository.findAllByMemberIdAndStatus(
                memberId, WalkThreadApplicationStatus.JOINED);
        List<Long> threadIds = applications.stream().map(WalkThreadApplication::getThreadId).toList();
        if (threadIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> threads = walkThreadRepository.findAllById(threadIds).stream()
                .filter(thread -> thread.getStatus() == WalkThreadStatus.RECRUITING)
                .filter(thread -> !thread.isExpired(now))
                .filter(thread -> !thread.isAuthor(memberId))
                .toList();

        return toBatchSummaryResponses(threads, memberId);
    }

    private List<ThreadSummaryResponse> toBatchSummaryResponses(List<WalkThread> threads, Long memberId) {
        if (threads.isEmpty()) {
            return List.of();
        }
        List<Long> threadIds = threads.stream().map(WalkThread::getId).toList();
        Map<Long, Long> countMap = batchCountByStatus(threadIds, WalkThreadApplicationStatus.JOINED);
        Set<Long> appliedSet = batchAppliedSet(threadIds, memberId, WalkThreadApplicationStatus.JOINED);

        // Batch fetch first pet image for each thread (same logic as getMapThreads)
        List<WalkThreadPet> allThreadPets = walkThreadPetRepository.findAllByThreadIdIn(threadIds);
        Map<Long, Long> threadFirstPetMap = new HashMap<>();
        for (WalkThreadPet tp : allThreadPets) {
            threadFirstPetMap.putIfAbsent(tp.getThreadId(), tp.getPetId());
        }
        List<Long> petIds = new ArrayList<>(new HashSet<>(threadFirstPetMap.values()));
        Map<Long, String> petPhotoMap = new HashMap<>();
        if (!petIds.isEmpty()) {
            for (Pet pet : petRepository.findAllById(petIds)) {
                if (pet.getPhotoUrl() != null) {
                    petPhotoMap.put(pet.getId(), pet.getPhotoUrl());
                }
            }
        }

        return threads.stream()
                .map(thread -> {
                    Long firstPetId = threadFirstPetMap.get(thread.getId());
                    String petImageUrl = firstPetId != null ? petPhotoMap.get(firstPetId) : null;
                    return toSummaryResponse(thread, countMap.getOrDefault(thread.getId(), 0L) + 1, appliedSet.contains(thread.getId()), petImageUrl);
                })
                .toList();
    }

    private ThreadSummaryResponse toSummaryResponse(WalkThread thread, long currentParticipants, boolean isApplied, String petImageUrl) {
        return ThreadSummaryResponse.builder()
                .id(thread.getId())
                .title(thread.getTitle())
                .description(thread.getDescription())
                .chatType(thread.getChatType().name())
                .maxParticipants(thread.getMaxParticipants())
                .currentParticipants((int) currentParticipants)
                .placeName(thread.getPlaceName())
                .latitude(thread.getLatitude())
                .longitude(thread.getLongitude())
                .startTime(thread.getStartTime())
                .endTime(thread.getEndTime())
                .status(thread.getStatus().name())
                .petImageUrl(petImageUrl)
                .isApplied(isApplied)
                .build();
    }

    private Map<Long, Long> batchCountByStatus(List<Long> threadIds, WalkThreadApplicationStatus status) {
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : walkThreadApplicationRepository.countByThreadIdInAndStatus(threadIds, status)) {
            countMap.put((Long) row[0], (Long) row[1]);
        }
        return countMap;
    }

    private Set<Long> batchAppliedSet(List<Long> threadIds, Long memberId, WalkThreadApplicationStatus status) {
        Set<Long> appliedSet = new HashSet<>();
        for (WalkThreadApplication app : walkThreadApplicationRepository.findByThreadIdInAndMemberIdAndStatus(threadIds, memberId, status)) {
            appliedSet.add(app.getThreadId());
        }
        return appliedSet;
    }

    private ThreadResponse toThreadResponse(WalkThread thread, Long memberId) {
        long currentParticipants = walkThreadApplicationRepository.countByThreadIdAndStatus(
                thread.getId(),
                WalkThreadApplicationStatus.JOINED
        ) + 1;

        // Collect author pet IDs
        List<Long> authorPetIds = walkThreadPetRepository.findAllByThreadId(thread.getId())
                .stream()
                .map(WalkThreadPet::getPetId)
                .toList();

        // Collect joined applications (reused for applicants + application pets)
        List<WalkThreadApplication> joinedApplications = walkThreadApplicationRepository
                .findAllByThreadIdAndStatus(thread.getId(), WalkThreadApplicationStatus.JOINED);

        // Collect all pet IDs: author pets + joined applicants' pets
        Set<Long> allPetIds = new java.util.LinkedHashSet<>(authorPetIds);
        if (!joinedApplications.isEmpty()) {
            List<Long> applicationIds = joinedApplications.stream()
                    .map(WalkThreadApplication::getId)
                    .toList();
            List<WalkThreadApplicationPet> applicationPets = walkThreadApplicationPetRepository
                    .findAllByApplicationIdIn(applicationIds);
            for (WalkThreadApplicationPet ap : applicationPets) {
                allPetIds.add(ap.getPetId());
            }
        }

        // Build PetSummary list
        List<ThreadResponse.PetSummary> pets = List.of();
        if (!allPetIds.isEmpty()) {
            List<Pet> petEntities = petRepository.findAllById(new ArrayList<>(allPetIds));
            pets = petEntities.stream()
                    .map(pet -> ThreadResponse.PetSummary.builder()
                            .id(pet.getId())
                            .name(pet.getName())
                            .photoUrl(pet.getPhotoUrl())
                            .breedName(pet.getBreed() != null ? pet.getBreed().getName() : null)
                            .age(pet.getAge())
                            .gender(pet.getGender() != null ? pet.getGender().name() : null)
                            .size(pet.getSize() != null ? pet.getSize().name() : null)
                            .mbti(pet.getMbti())
                            .isNeutered(pet.getIsNeutered())
                            .walkingStyles(pet.getPetWalkingStyles().stream()
                                    .map(ws -> ws.getWalkingStyle().getName())
                                    .toList())
                            .personalities(pet.getPetPersonalities().stream()
                                    .map(pp -> pp.getPersonality().getName())
                                    .toList())
                            .build())
                    .toList();
        }

        // Applicants (only for thread author)
        List<ThreadResponse.ApplicantSummary> applicants = null;
        if (thread.isAuthor(memberId)) {
            applicants = joinedApplications.stream()
                    .map(application -> ThreadResponse.ApplicantSummary.builder()
                            .memberId(application.getMemberId())
                            .status(application.getStatus().name())
                            .chatRoomId(application.getChatRoomId())
                            .build())
                    .toList();
        }

        boolean isApplied = walkThreadApplicationRepository
                .findByThreadIdAndMemberIdAndStatus(thread.getId(), memberId, WalkThreadApplicationStatus.JOINED)
                .isPresent();

        return ThreadResponse.builder()
                .id(thread.getId())
                .authorId(thread.getAuthorId())
                .title(thread.getTitle())
                .description(thread.getDescription())
                .walkDate(thread.getWalkDate())
                .startTime(thread.getStartTime())
                .endTime(thread.getEndTime())
                .chatType(thread.getChatType().name())
                .maxParticipants(thread.getMaxParticipants())
                .currentParticipants((int) currentParticipants)
                .allowNonPetOwner(thread.getAllowNonPetOwner())
                .isVisibleAlways(thread.getIsVisibleAlways())
                .placeName(thread.getPlaceName())
                .latitude(thread.getLatitude())
                .longitude(thread.getLongitude())
                .address(thread.getAddress())
                .status(thread.getStatus().name())
                .petIds(authorPetIds)
                .pets(pets)
                .applicants(applicants)
                .applied(isApplied)
                .build();
    }

    private void validateActiveThreadLimit(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> threads = walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING);
        for (WalkThread thread : threads) {
            if (thread.isExpired(now)) {
                thread.expire();
                continue;
            }
            throw new BusinessException(ThreadErrorCode.THREAD_ALREADY_ACTIVE);
        }
    }

    private void saveThreadPets(Long threadId, List<Long> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            throw new BusinessException(ThreadErrorCode.INVALID_THREAD_REQUEST);
        }
        for (Long petId : petIds) {
            walkThreadPetRepository.save(WalkThreadPet.of(threadId, petId));
        }
    }

    private void saveApplicationPets(Long applicationId, List<Long> petIds) {
        if (petIds == null || petIds.isEmpty()) {
            return;
        }
        for (Long petId : petIds) {
            walkThreadApplicationPetRepository.save(WalkThreadApplicationPet.of(applicationId, petId));
        }
    }

    private void saveThreadFilters(Long threadId, List<ThreadCreateRequest.ThreadFilterRequest> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        for (ThreadCreateRequest.ThreadFilterRequest filter : filters) {
            String serializedValues = filter.getValues() == null ? "[]" : filter.getValues().toString();
            WalkThreadFilter entity = WalkThreadFilter.of(
                    threadId,
                    filter.getType(),
                    serializedValues,
                    filter.getIsRequired() != null ? filter.getIsRequired() : Boolean.FALSE
            );
            walkThreadFilterRepository.save(entity);
        }
    }

    private WalkChatType resolveChatType(String rawChatType) {
        if (rawChatType == null) {
            throw new BusinessException(ThreadErrorCode.INVALID_CHAT_TYPE);
        }
        try {
            return WalkChatType.valueOf(rawChatType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ThreadErrorCode.INVALID_CHAT_TYPE);
        }
    }

    private Long resolveChatRoomId(WalkThread thread, Long applicantId) {
        Long threadId = thread.getId();
        Long authorId = thread.getAuthorId();

        ChatRoom room = switch (thread.getChatType()) {
            case GROUP -> chatRoomRepository
                    .findFirstByThreadIdAndChatTypeAndStatusOrderByIdAsc(
                            threadId,
                            ChatRoomType.GROUP,
                            ChatRoomStatus.ACTIVE
                    )
                    .orElseGet(() -> chatRoomRepository.save(
                            ChatRoom.create(threadId, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE,
                                    ChatRoomOrigin.WALK, thread.getTitle())
                    ));
            case INDIVIDUAL -> chatRoomRepository
                    .findByThreadIdAndTypeAndParticipants(
                            threadId,
                            ChatRoomType.DIRECT,
                            ChatRoomStatus.ACTIVE,
                            authorId,
                            applicantId
                    )
                    .orElseGet(() -> chatRoomRepository.save(
                            ChatRoom.create(threadId, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE,
                                    ChatRoomOrigin.WALK, thread.getTitle())
                    ));
        };

        ensureParticipantJoined(room.getId(), authorId);
        ensureParticipantJoined(room.getId(), applicantId);

        return room.getId();
    }

    private void ensureParticipantJoined(Long chatRoomId, Long memberId) {
        Optional<ChatParticipant> participantOptional = chatParticipantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);
        if (participantOptional.isPresent()) {
            ChatParticipant participant = participantOptional.get();
            if (participant.isLeft()) {
                participant.rejoin();
            }
            return;
        }
        chatParticipantRepository.save(ChatParticipant.create(chatRoomId, memberId));
    }

    private BigDecimal toDecimal(Double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
