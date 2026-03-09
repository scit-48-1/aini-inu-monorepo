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
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.notification.entity.NotificationType;
import scit.ainiinu.notification.service.NotificationService;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkConfirmService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final WalkThreadRepository walkThreadRepository;
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Transactional
    public WalkConfirmResponse confirmWalk(Long memberId, Long chatRoomId) {
        chatRoomRepository.findByIdForUpdate(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));

        ChatParticipant me = chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED));

        me.confirmWalk();
        WalkConfirmResponse response = buildResponse(chatRoomId, memberId, me, true);

        // 산책 완료 확인 알림 발행
        publishWalkConfirmNotifications(memberId, chatRoomId, response);

        return response;
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

    private void publishWalkConfirmNotifications(Long confirmerId, Long chatRoomId, WalkConfirmResponse response) {
        List<ChatParticipant> activeParticipants = chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(chatRoomId);
        Member confirmer = memberRepository.findById(confirmerId).orElse(null);
        String confirmerNickname = confirmer != null ? confirmer.getNickname() : "알 수 없는 사용자";

        if (response.isAllConfirmed()) {
            for (ChatParticipant p : activeParticipants) {
                notificationService.createAndPublish(
                        p.getMemberId(),
                        NotificationType.WALK_CONFIRM,
                        "산책 완료",
                        "모든 참여자가 산책 완료를 확인했습니다!",
                        chatRoomId,
                        "CHAT_ROOM"
                );
            }
        } else {
            for (ChatParticipant p : activeParticipants) {
                if (!p.getMemberId().equals(confirmerId)) {
                    notificationService.createAndPublish(
                            p.getMemberId(),
                            NotificationType.WALK_CONFIRM,
                            "산책 완료 확인",
                            confirmerNickname + "님이 산책 완료를 확인했습니다.",
                            chatRoomId,
                            "CHAT_ROOM"
                    );
                }
            }
        }
    }
}
