package scit.ainiinu.walk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
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
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.ThreadErrorCode;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadFilterRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkThreadService {

    private final WalkThreadRepository walkThreadRepository;
    private final WalkThreadPetRepository walkThreadPetRepository;
    private final WalkThreadFilterRepository walkThreadFilterRepository;
    private final WalkThreadApplicationRepository walkThreadApplicationRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;

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

        return toThreadResponse(savedThread, memberId);
    }

    public SliceResponse<ThreadSummaryResponse> getThreads(Long memberId, Pageable pageable) {
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Slice<WalkThread> slice = walkThreadRepository.findByStatusOrderByCreatedAtDescIdDesc(
                WalkThreadStatus.RECRUITING,
                safePageable
        );

        LocalDateTime now = LocalDateTime.now();
        List<ThreadSummaryResponse> content = slice.getContent().stream()
                .filter(thread -> !thread.isExpired(now))
                .map(thread -> toSummaryResponse(thread, memberId))
                .toList();
        Slice<ThreadSummaryResponse> filtered = new org.springframework.data.domain.SliceImpl<>(
                content, safePageable, slice.hasNext());
        return SliceResponse.of(filtered);
    }

    public List<ThreadMapResponse> getMapThreads(Long memberId, double latitude, double longitude, double radiusKm) {
        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> recruitingThreads = walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING);

        List<ThreadMapResponse> results = new ArrayList<>();
        for (WalkThread thread : recruitingThreads) {
            if (thread.isExpired(now)) {
                continue;
            }
            double distance = distanceInKm(latitude, longitude, thread.getLatitude().doubleValue(), thread.getLongitude().doubleValue());
            if (distance > radiusKm) {
                continue;
            }
            long currentParticipants = walkThreadApplicationRepository.countByThreadIdAndStatus(
                    thread.getId(),
                    WalkThreadApplicationStatus.JOINED
            );
            results.add(ThreadMapResponse.builder()
                    .threadId(thread.getId())
                    .title(thread.getTitle())
                    .chatType(thread.getChatType().name())
                    .currentParticipants((int) currentParticipants)
                    .maxParticipants(thread.getMaxParticipants())
                    .latitude(thread.getLatitude())
                    .longitude(thread.getLongitude())
                    .placeName(thread.getPlaceName())
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

        if (request.getPetIds() != null) {
            walkThreadPetRepository.deleteAllByThreadId(threadId);
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
        if (joinedCount >= thread.getMaxParticipants()) {
            throw new BusinessException(ThreadErrorCode.CAPACITY_FULL);
        }

        Long chatRoomId = resolveChatRoomId(thread, memberId);
        if (existing.isPresent()) {
            existing.get().rejoin(chatRoomId);
        } else {
            WalkThreadApplication application = WalkThreadApplication.joined(threadId, memberId, chatRoomId);
            walkThreadApplicationRepository.save(application);
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
            countByRegion.merge(thread.getPlaceName(), 1L, Long::sum);
        }

        List<ThreadHotspotResponse> responses = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByRegion.entrySet()) {
            responses.add(new ThreadHotspotResponse(entry.getKey(), entry.getValue()));
        }
        responses.sort(Comparator.comparing(ThreadHotspotResponse::getCount).reversed());
        return responses;
    }

    public List<ThreadSummaryResponse> getMyActiveThread(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<WalkThread> threads = walkThreadRepository.findAllByAuthorIdAndStatus(memberId, WalkThreadStatus.RECRUITING);
        return threads.stream()
                .filter(thread -> !thread.isExpired(now))
                .map(thread -> toSummaryResponse(thread, memberId))
                .toList();
    }

    private ThreadSummaryResponse toSummaryResponse(WalkThread thread, Long memberId) {
        long currentParticipants = walkThreadApplicationRepository.countByThreadIdAndStatus(
                thread.getId(),
                WalkThreadApplicationStatus.JOINED
        );

        boolean isApplied = walkThreadApplicationRepository
                .findByThreadIdAndMemberIdAndStatus(thread.getId(), memberId, WalkThreadApplicationStatus.JOINED)
                .isPresent();

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
                .isApplied(isApplied)
                .build();
    }

    private ThreadResponse toThreadResponse(WalkThread thread, Long memberId) {
        long currentParticipants = walkThreadApplicationRepository.countByThreadIdAndStatus(
                thread.getId(),
                WalkThreadApplicationStatus.JOINED
        );

        List<Long> petIds = walkThreadPetRepository.findAllByThreadId(thread.getId())
                .stream()
                .map(WalkThreadPet::getPetId)
                .toList();

        List<ThreadResponse.ApplicantSummary> applicants = null;
        if (thread.isAuthor(memberId)) {
            applicants = walkThreadApplicationRepository.findAllByThreadIdAndStatus(
                            thread.getId(),
                            WalkThreadApplicationStatus.JOINED
                    ).stream()
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
                .petIds(petIds)
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
                            ChatRoom.create(threadId, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE)
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
                            ChatRoom.create(threadId, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE)
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
