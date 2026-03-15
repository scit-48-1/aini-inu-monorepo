package scit.ainiinu.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.dto.request.ChatRoomDirectCreateRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.ChatParticipantPetResponse;
import scit.ainiinu.chat.dto.response.ChatParticipantResponse;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.dto.response.ChatRoomSummaryResponse;
import scit.ainiinu.chat.dto.response.ChatSenderResponse;
import scit.ainiinu.chat.dto.response.LeaveRoomResponse;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatParticipantPet;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantPetRepository;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.dashboard.dto.response.DashboardPendingReviewResponse;
import scit.ainiinu.dashboard.dto.response.DashboardRecentFriendResponse;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.repository.PetRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import scit.ainiinu.member.entity.Member;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatParticipantPetRepository chatParticipantPetRepository;
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final PetRepository petRepository;
    private final ChatReviewRepository chatReviewRepository;

    public SliceResponse<ChatRoomSummaryResponse> getRooms(Long memberId, String status, String origin, Pageable pageable) {
        ChatRoomStatus parsedStatus = parseStatus(status);
        ChatRoomOrigin parsedOrigin = parseOrigin(origin);
        Slice<ChatRoom> rooms = chatRoomRepository.findAccessibleRoomsByMemberId(memberId, parsedStatus, parsedOrigin, pageable);

        // Batch-fetch last messages for all rooms (N+1 prevention)
        List<Long> roomIds = rooms.getContent().stream().map(ChatRoom::getId).toList();
        Map<Long, Message> lastMessages = messageRepository.findLastMessagesByRoomIds(roomIds);

        // Batch-fetch participants and member nicknames for display name
        List<ChatParticipant> allParticipants = roomIds.isEmpty()
                ? Collections.emptyList()
                : chatParticipantRepository.findAllByChatRoomIdIn(roomIds);

        List<Long> memberIds = allParticipants.stream()
                .map(ChatParticipant::getMemberId)
                .distinct()
                .toList();
        Map<Long, Member> membersById = memberIds.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findAllById(memberIds).stream()
                        .collect(Collectors.toMap(Member::getId, m -> m));
        Map<Long, String> nicknamesByMemberId = membersById.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getNickname()));

        Map<Long, List<ChatParticipant>> participantsByRoom = allParticipants.stream()
                .collect(Collectors.groupingBy(ChatParticipant::getChatRoomId));

        // Batch-fetch unread counts (N+1 prevention)
        Map<Long, Long> unreadCounts = roomIds.isEmpty()
                ? Collections.emptyMap()
                : messageRepository.countUnreadByRoomIds(memberId, roomIds);

        Slice<ChatRoomSummaryResponse> mapped = rooms.map(room -> {
            List<ChatParticipant> roomParticipants = participantsByRoom.getOrDefault(room.getId(), List.of());
            String displayName = computeDisplayName(memberId, roomParticipants, nicknamesByMemberId);
            List<String> profileImages = roomParticipants.stream()
                    .filter(p -> !p.getMemberId().equals(memberId) && !p.isLeft())
                    .limit(4)
                    .map(p -> {
                        Member m = membersById.get(p.getMemberId());
                        return m != null ? m.getProfileImageUrl() : null;
                    })
                    .toList();
            int unreadCount = unreadCounts.getOrDefault(room.getId(), 0L).intValue();
            return toSummaryResponse(room, lastMessages.get(room.getId()), displayName, profileImages, unreadCount);
        });
        return SliceResponse.of(mapped);
    }

    @Transactional
    public ChatRoomDetailResponse createDirectRoom(Long memberId, ChatRoomDirectCreateRequest request) {
        Long partnerId = request.getPartnerId();
        if (partnerId == null || partnerId.equals(memberId)) {
            throw new ChatException(ChatErrorCode.INVALID_REQUEST);
        }
        if (!memberRepository.existsById(partnerId)) {
            throw new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED);
        }

        ChatRoomOrigin requestOrigin = parseOrigin(request.getOrigin());

        List<ChatRoom> existingRooms = chatRoomRepository.findDirectRoomsByParticipants(
                ChatRoomType.DIRECT, memberId, partnerId, requestOrigin);

        if (!existingRooms.isEmpty()) {
            ChatRoom room = existingRooms.get(0);
            if (room.isClosed()) {
                room.reopen();
            }
            rejoinIfLeft(room.getId(), memberId);
            rejoinIfLeft(room.getId(), partnerId);
            return toDetailResponse(room);
        }
        ChatRoom room = chatRoomRepository.save(
                ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE,
                        requestOrigin != null ? requestOrigin : ChatRoomOrigin.DM, request.getRoomTitle()));
        ChatParticipant me = chatParticipantRepository.save(ChatParticipant.create(room.getId(), memberId));
        ChatParticipant partner = chatParticipantRepository.save(ChatParticipant.create(room.getId(), partnerId));

        saveParticipantPets(me);
        saveParticipantPets(partner);

        return toDetailResponse(room);
    }

    public ChatRoomDetailResponse getRoomDetail(Long memberId, Long chatRoomId) {
        validateRoomParticipant(memberId, chatRoomId);
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        return toDetailResponse(room);
    }

    public List<DashboardRecentFriendResponse> getDashboardRecentFriends(Long memberId, int limit) {
        int safeLimit = Math.max(limit, 1);
        List<ChatRoom> rooms = chatRoomRepository.findAccessibleRoomsByMemberId(
                memberId,
                null,
                null,
                PageRequest.of(0, safeLimit)
        ).getContent();
        if (rooms.isEmpty()) {
            return List.of();
        }

        DashboardRoomContext context = buildDashboardRoomContext(rooms);
        Set<Long> seenMembers = new HashSet<>();
        List<DashboardRecentFriendResponse> friends = new ArrayList<>();

        for (ChatRoom room : rooms) {
            ChatParticipant partner = findActivePartner(memberId, context.participantsByRoom.getOrDefault(room.getId(), List.of()));
            if (partner == null || !seenMembers.add(partner.getMemberId())) {
                continue;
            }

            Member member = context.membersById.get(partner.getMemberId());
            List<String> petNames = context.petNamesByParticipantId.getOrDefault(partner.getId(), List.of());
            String displayName = !petNames.isEmpty()
                    ? String.join(", ", petNames)
                    : resolveNickname(member, partner.getMemberId());

            friends.add(DashboardRecentFriendResponse.builder()
                    .memberId(partner.getMemberId())
                    .chatRoomId(room.getId())
                    .displayName(displayName)
                    .profileImageUrl(member != null ? member.getProfileImageUrl() : null)
                    .score(7.0)
                    .build());
        }

        return friends;
    }

    public List<DashboardPendingReviewResponse> getDashboardPendingReviews(Long memberId, int limit) {
        int safeLimit = Math.max(limit, 1);
        List<ChatRoom> rooms = chatRoomRepository.findAccessibleRoomsByMemberId(
                memberId,
                null,
                ChatRoomOrigin.WALK,
                PageRequest.of(0, safeLimit)
        ).getContent();
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<ChatRoom> walkConfirmedRooms = rooms.stream()
                .filter(room -> Boolean.TRUE.equals(room.getWalkConfirmed()))
                .toList();
        if (walkConfirmedRooms.isEmpty()) {
            return List.of();
        }

        DashboardRoomContext context = buildDashboardRoomContext(walkConfirmedRooms);
        List<Long> roomIds = walkConfirmedRooms.stream().map(ChatRoom::getId).toList();
        Set<Long> reviewedRoomIds = chatReviewRepository.findByReviewerIdAndChatRoomIdIn(memberId, roomIds).stream()
                .map(review -> review.getChatRoomId())
                .collect(Collectors.toSet());
        List<DashboardPendingReviewResponse> pendingReviews = new ArrayList<>();

        for (ChatRoom room : walkConfirmedRooms) {
            if (reviewedRoomIds.contains(room.getId())) {
                continue;
            }

            List<ChatParticipant> participants = context.participantsByRoom.getOrDefault(room.getId(), List.of());
            ChatParticipant partner = findActivePartner(memberId, participants);
            if (partner == null) {
                continue;
            }

            Member member = context.membersById.get(partner.getMemberId());
            pendingReviews.add(DashboardPendingReviewResponse.builder()
                    .chatRoomId(room.getId())
                    .displayName(computeDisplayName(memberId, participants, context.nicknamesByMemberId))
                    .partnerId(partner.getMemberId())
                    .partnerNickname(resolveNickname(member, partner.getMemberId()))
                    .profileImageUrl(member != null ? member.getProfileImageUrl() : null)
                    .build());
        }

        return pendingReviews;
    }

    @Transactional
    public LeaveRoomResponse leaveRoom(Long memberId, Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findByIdForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));

        Optional<ChatParticipant> participantOptional = chatParticipantRepository
                .findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId);

        if (participantOptional.isEmpty()) {
            Optional<ChatParticipant> anyParticipant = chatParticipantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId);
            if (anyParticipant.isPresent() && anyParticipant.get().isLeft()) {
                throw new ChatException(ChatErrorCode.ROOM_ALREADY_LEFT);
            }
            throw new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED);
        }

        ChatParticipant participant = participantOptional.get();
        participant.leave();

        long activeParticipants = chatParticipantRepository.countByChatRoomIdAndLeftAtIsNull(chatRoomId);
        if (activeParticipants == 0) {
            room.close();
        }

        return LeaveRoomResponse.builder()
                .roomId(chatRoomId)
                .left(true)
                .roomStatus(room.getStatus().name())
                .build();
    }

    private void rejoinIfLeft(Long chatRoomId, Long memberId) {
        chatParticipantRepository.findByChatRoomIdAndMemberId(chatRoomId, memberId)
                .ifPresent(p -> { if (p.isLeft()) p.rejoin(); });
    }

    private void saveParticipantPets(ChatParticipant participant) {
        List<Pet> pets = petRepository.findAllByMemberIdOrderByIsMainDesc(participant.getMemberId());
        for (Pet pet : pets) {
            chatParticipantPetRepository.save(ChatParticipantPet.of(participant.getId(), pet.getId()));
        }
    }

    private void validateRoomParticipant(Long memberId, Long chatRoomId) {
        boolean isParticipant = chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId);
        if (!isParticipant) {
            throw new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED);
        }
    }

    private DashboardRoomContext buildDashboardRoomContext(List<ChatRoom> rooms) {
        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        List<ChatParticipant> allParticipants = roomIds.isEmpty()
                ? Collections.emptyList()
                : chatParticipantRepository.findAllByChatRoomIdIn(roomIds);

        List<Long> memberIds = allParticipants.stream()
                .map(ChatParticipant::getMemberId)
                .distinct()
                .toList();
        Map<Long, Member> membersById = memberIds.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findAllById(memberIds).stream()
                        .collect(Collectors.toMap(Member::getId, member -> member));
        Map<Long, String> nicknamesByMemberId = membersById.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getNickname()));
        Map<Long, List<ChatParticipant>> participantsByRoom = allParticipants.stream()
                .collect(Collectors.groupingBy(ChatParticipant::getChatRoomId));

        List<Long> participantIds = allParticipants.stream().map(ChatParticipant::getId).toList();
        List<ChatParticipantPet> participantPets = participantIds.isEmpty()
                ? Collections.emptyList()
                : chatParticipantPetRepository.findAllByChatParticipantIdIn(participantIds);
        List<Long> petIds = participantPets.stream()
                .map(ChatParticipantPet::getPetId)
                .distinct()
                .toList();
        Map<Long, Pet> petsById = petIds.isEmpty()
                ? Collections.emptyMap()
                : petRepository.findAllById(petIds).stream()
                        .collect(Collectors.toMap(Pet::getId, pet -> pet));

        Map<Long, List<String>> petNamesByParticipantId = new HashMap<>();
        for (ChatParticipantPet participantPet : participantPets) {
            Pet pet = petsById.get(participantPet.getPetId());
            if (pet == null) {
                continue;
            }
            petNamesByParticipantId
                    .computeIfAbsent(participantPet.getChatParticipantId(), key -> new ArrayList<>())
                    .add(pet.getName());
        }

        return new DashboardRoomContext(
                participantsByRoom,
                membersById,
                nicknamesByMemberId,
                petNamesByParticipantId
        );
    }

    private ChatParticipant findActivePartner(Long memberId, List<ChatParticipant> participants) {
        List<ChatParticipant> sortedParticipants = new ArrayList<>(participants);
        sortedParticipants.sort(Comparator.comparing(ChatParticipant::getId));
        for (ChatParticipant participant : sortedParticipants) {
            if (!participant.getMemberId().equals(memberId) && !participant.isLeft()) {
                return participant;
            }
        }
        return null;
    }

    private String resolveNickname(Member member, Long memberId) {
        if (member != null && member.getNickname() != null && !member.getNickname().isBlank()) {
            return member.getNickname();
        }
        return "Member " + memberId;
    }

    private String computeDisplayName(Long memberId, List<ChatParticipant> participants, Map<Long, String> nicknamesByMemberId) {
        List<String> otherNames = participants.stream()
                .filter(p -> !p.getMemberId().equals(memberId) && !p.isLeft())
                .map(p -> nicknamesByMemberId.getOrDefault(p.getMemberId(), "Member " + p.getMemberId()))
                .toList();

        if (otherNames.isEmpty()) {
            return "알 수 없음";
        }
        if (otherNames.size() <= 2) {
            return String.join(", ", otherNames);
        }
        return otherNames.get(0) + ", " + otherNames.get(1) + " 외 " + (otherNames.size() - 2) + "명";
    }

    private ChatRoomSummaryResponse toSummaryResponse(ChatRoom room, Message lastMsg, String displayName, List<String> participantProfileImages, int unreadCount) {
        ChatMessageResponse lastMessage = lastMsg != null ? toMessageResponse(lastMsg) : null;

        return ChatRoomSummaryResponse.builder()
                .chatRoomId(room.getId())
                .chatType(room.getChatType().name())
                .status(room.getStatus().name())
                .origin(room.getOrigin().name())
                .roomTitle(room.getRoomTitle())
                .displayName(displayName)
                .participantProfileImages(participantProfileImages)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private ChatRoomDetailResponse toDetailResponse(ChatRoom room) {
        List<ChatParticipant> participants = new ArrayList<>(chatParticipantRepository.findAllByChatRoomId(room.getId()));
        participants.sort(Comparator.comparing(ChatParticipant::getId));

        List<Long> memberIds = participants.stream().map(ChatParticipant::getMemberId).distinct().toList();
        Map<Long, Member> membersById = memberIds.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findAllById(memberIds).stream()
                        .collect(Collectors.toMap(Member::getId, member -> member));

        List<Long> participantIds = participants.stream().map(ChatParticipant::getId).toList();
        List<ChatParticipantPet> participantPets = participantIds.isEmpty()
                ? Collections.emptyList()
                : chatParticipantPetRepository.findAllByChatParticipantIdIn(participantIds);

        List<Long> petIds = participantPets.stream().map(ChatParticipantPet::getPetId).distinct().toList();
        Map<Long, Pet> petsById = petIds.isEmpty()
                ? Collections.emptyMap()
                : petRepository.findAllById(petIds).stream()
                        .collect(Collectors.toMap(Pet::getId, pet -> pet));

        Map<Long, List<ChatParticipantPetResponse>> petResponseByParticipant = new HashMap<>();
        for (ChatParticipantPet participantPet : participantPets) {
            Pet pet = petsById.get(participantPet.getPetId());
            ChatParticipantPetResponse petResponse = ChatParticipantPetResponse.builder()
                    .petId(participantPet.getPetId())
                    .name(pet != null ? pet.getName() : null)
                    .build();
            petResponseByParticipant
                    .computeIfAbsent(participantPet.getChatParticipantId(), key -> new ArrayList<>())
                    .add(petResponse);
        }

        List<ChatParticipantResponse> participantResponses = participants.stream()
                .map(participant -> {
                    Member member = membersById.get(participant.getMemberId());
                    return ChatParticipantResponse.builder()
                            .memberId(participant.getMemberId())
                            .nickname(member != null ? member.getNickname() : null)
                            .profileImageUrl(member != null ? member.getProfileImageUrl() : null)
                            .walkConfirmState(participant.getWalkConfirmState().name())
                            .left(participant.isLeft())
                            .pets(petResponseByParticipant.getOrDefault(participant.getId(), List.of()))
                            .build();
                })
                .toList();

        ChatMessageResponse lastMessage = messageRepository.findTopByChatRoomIdOrderByIdDesc(room.getId())
                .map(this::toMessageResponse)
                .orElse(null);

        return ChatRoomDetailResponse.builder()
                .chatRoomId(room.getId())
                .chatType(room.getChatType().name())
                .status(room.getStatus().name())
                .origin(room.getOrigin().name())
                .threadId(room.getThreadId())
                .roomTitle(room.getRoomTitle())
                .walkConfirmed(Boolean.TRUE.equals(room.getWalkConfirmed()))
                .participants(participantResponses)
                .lastMessage(lastMessage)
                .build();
    }

    private ChatMessageResponse toMessageResponse(Message message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoomId())
                .sender(ChatSenderResponse.of(message.getSenderId()))
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .status("CREATED")
                .clientMessageId(message.getClientMessageId())
                .sentAt(message.getSentAt())
                .build();
    }

    private ChatRoomStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ChatRoomStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ChatException(ChatErrorCode.INVALID_REQUEST);
        }
    }

    private ChatRoomOrigin parseOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        try {
            return ChatRoomOrigin.valueOf(origin.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ChatException(ChatErrorCode.INVALID_REQUEST);
        }
    }

    private record DashboardRoomContext(
            Map<Long, List<ChatParticipant>> participantsByRoom,
            Map<Long, Member> membersById,
            Map<Long, String> nicknamesByMemberId,
            Map<Long, List<String>> petNamesByParticipantId
    ) {
    }
}
