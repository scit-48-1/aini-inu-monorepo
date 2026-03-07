package scit.ainiinu.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.dto.request.WalkConfirmRequest;
import scit.ainiinu.chat.dto.response.WalkConfirmResponse;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatWalkConfirmState;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkConfirmService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final WalkThreadRepository walkThreadRepository;

    @Transactional
    public WalkConfirmResponse confirmWalk(Long memberId, Long chatRoomId) {
        chatRoomRepository.findByIdForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));

        ChatParticipant me = chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED));

        me.confirmWalk();
        return buildResponse(chatRoomId, memberId, me, true);
    }

    @Transactional
    public void cancelWalkConfirm(Long memberId, Long chatRoomId) {
        chatRoomRepository.findByIdForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));

        ChatParticipant me = chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED));

        me.cancelWalkConfirm();
        buildResponse(chatRoomId, memberId, me, true);
    }

    public WalkConfirmResponse getWalkConfirm(Long memberId, Long chatRoomId) {
        chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));

        ChatParticipant me = chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED));

        return buildResponse(chatRoomId, memberId, me, false);
    }

    @Transactional
    public WalkConfirmResponse updateWalkConfirm(Long memberId, Long chatRoomId, WalkConfirmRequest request) {
        if (request == null || request.getAction() == null || request.getAction().isBlank()) {
            return confirmWalk(memberId, chatRoomId);
        }
        String action = request.getAction().trim().toUpperCase();
        return switch (action) {
            case "CONFIRM" -> confirmWalk(memberId, chatRoomId);
            case "CANCEL" -> {
                cancelWalkConfirm(memberId, chatRoomId);
                yield getWalkConfirm(memberId, chatRoomId);
            }
            default -> throw new ChatException(ChatErrorCode.INVALID_WALK_CONFIRM_ACTION);
        };
    }

    private WalkConfirmResponse buildResponse(Long chatRoomId, Long memberId, ChatParticipant me, boolean syncRoomState) {
        List<ChatParticipant> activeParticipants = chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(chatRoomId);
        boolean allConfirmed = !activeParticipants.isEmpty()
                && activeParticipants.stream().allMatch(p -> p.getWalkConfirmState() == ChatWalkConfirmState.CONFIRMED);

        if (syncRoomState) {
            chatRoomRepository.findById(chatRoomId)
                    .ifPresent(room -> {
                        room.updateWalkConfirmed(allConfirmed);
                        if (allConfirmed && room.getThreadId() != null) {
                            walkThreadRepository.findById(room.getThreadId())
                                    .ifPresent(thread -> thread.complete());
                        }
                    });
        }

        List<Long> confirmedMemberIds = activeParticipants.stream()
                .filter(participant -> participant.getWalkConfirmState() == ChatWalkConfirmState.CONFIRMED)
                .map(ChatParticipant::getMemberId)
                .toList();

        return WalkConfirmResponse.builder()
                .roomId(chatRoomId)
                .memberId(memberId)
                .myState(me.getWalkConfirmState().name())
                .allConfirmed(allConfirmed)
                .confirmedMemberIds(confirmedMemberIds)
                .build();
    }
}
