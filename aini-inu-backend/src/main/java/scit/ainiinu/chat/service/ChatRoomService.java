package scit.ainiinu.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantPetRepository;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.repository.PetRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public SliceResponse<ChatRoomSummaryResponse> getRooms(Long memberId, String status, Pageable pageable) {
        ChatRoomStatus parsedStatus = parseStatus(status);
        Slice<ChatRoom> rooms = chatRoomRepository.findAccessibleRoomsByMemberId(memberId, parsedStatus, pageable);

        // Batch-fetch last messages for all rooms (N+1 prevention)
        List<Long> roomIds = rooms.getContent().stream().map(ChatRoom::getId).toList();
        Map<Long, Message> lastMessages = messageRepository.findLastMessagesByRoomIds(roomIds);

        Slice<ChatRoomSummaryResponse> mapped = rooms.map(room -> toSummaryResponse(room, lastMessages.get(room.getId())));
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

        Optional<ChatRoom> existing = chatRoomRepository.findByTypeAndParticipants(
                ChatRoomType.DIRECT,
                ChatRoomStatus.ACTIVE,
                memberId,
                partnerId
        );

        if (existing.isPresent()) {
            return toDetailResponse(existing.get());
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE));
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

    private ChatRoomSummaryResponse toSummaryResponse(ChatRoom room, Message lastMsg) {
        ChatMessageResponse lastMessage = lastMsg != null ? toMessageResponse(lastMsg) : null;

        return ChatRoomSummaryResponse.builder()
                .chatRoomId(room.getId())
                .chatType(room.getChatType().name())
                .status(room.getStatus().name())
                .lastMessage(lastMessage)
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private ChatRoomDetailResponse toDetailResponse(ChatRoom room) {
        List<ChatParticipant> participants = new ArrayList<>(chatParticipantRepository.findAllByChatRoomId(room.getId()));
        participants.sort(Comparator.comparing(ChatParticipant::getId));

        List<Long> participantIds = participants.stream().map(ChatParticipant::getId).toList();
        List<ChatParticipantPet> participantPets = participantIds.isEmpty()
                ? Collections.emptyList()
                : chatParticipantPetRepository.findAllByChatParticipantIdIn(participantIds);

        List<Long> petIds = participantPets.stream().map(ChatParticipantPet::getPetId).distinct().toList();
        Map<Long, Pet> petsById = new HashMap<>();
        if (!petIds.isEmpty()) {
            for (Pet pet : petRepository.findAllById(petIds)) {
                petsById.put(pet.getId(), pet);
            }
        }

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
                .map(participant -> ChatParticipantResponse.builder()
                        .memberId(participant.getMemberId())
                        .walkConfirmState(participant.getWalkConfirmState().name())
                        .left(participant.isLeft())
                        .pets(petResponseByParticipant.getOrDefault(participant.getId(), List.of()))
                        .build())
                .toList();

        ChatMessageResponse lastMessage = messageRepository.findTopByChatRoomIdOrderByIdDesc(room.getId())
                .map(this::toMessageResponse)
                .orElse(null);

        return ChatRoomDetailResponse.builder()
                .chatRoomId(room.getId())
                .chatType(room.getChatType().name())
                .status(room.getStatus().name())
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
}
