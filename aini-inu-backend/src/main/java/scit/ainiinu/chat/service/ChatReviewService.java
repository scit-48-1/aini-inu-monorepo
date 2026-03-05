package scit.ainiinu.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.dto.request.ChatReviewCreateRequest;
import scit.ainiinu.chat.dto.response.ChatReviewResponse;
import scit.ainiinu.chat.dto.response.MyChatReviewResponse;
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.common.response.SliceResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReviewService {

    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatReviewRepository chatReviewRepository;

    @Transactional
    public ChatReviewResponse createReview(Long memberId, Long chatRoomId, ChatReviewCreateRequest request) {
        validateParticipant(memberId, chatRoomId);
        validateParticipant(request.getRevieweeId(), chatRoomId);
        if (memberId.equals(request.getRevieweeId())) {
            throw new ChatException(ChatErrorCode.INVALID_REQUEST);
        }

        boolean exists = chatReviewRepository.existsByChatRoomIdAndReviewerIdAndRevieweeId(
                chatRoomId,
                memberId,
                request.getRevieweeId()
        );
        if (exists) {
            throw new ChatException(ChatErrorCode.REVIEW_ALREADY_EXISTS);
        }

        ChatReview saved = chatReviewRepository.save(ChatReview.create(
                chatRoomId,
                memberId,
                request.getRevieweeId(),
                request.getScore(),
                request.getComment()
        ));
        return toResponse(saved);
    }

    public MyChatReviewResponse getMyReview(Long memberId, Long chatRoomId) {
        validateParticipant(memberId, chatRoomId);

        return chatReviewRepository.findTopByChatRoomIdAndReviewerIdOrderByCreatedAtDesc(chatRoomId, memberId)
                .map(review -> MyChatReviewResponse.builder().exists(true).review(toResponse(review)).build())
                .orElseGet(() -> MyChatReviewResponse.builder().exists(false).review(null).build());
    }

    public SliceResponse<ChatReviewResponse> getReviews(Long memberId, Long chatRoomId, Pageable pageable) {
        validateParticipant(memberId, chatRoomId);

        Slice<ChatReview> reviews = chatReviewRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(chatRoomId, pageable);
        Slice<ChatReviewResponse> mapped = reviews.map(this::toResponse);
        return SliceResponse.of(mapped);
    }

    private void validateParticipant(Long memberId, Long chatRoomId) {
        boolean isParticipant = chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId);
        if (!isParticipant) {
            throw new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED);
        }
    }

    private ChatReviewResponse toResponse(ChatReview review) {
        return ChatReviewResponse.builder()
                .id(review.getId())
                .chatRoomId(review.getChatRoomId())
                .reviewerId(review.getReviewerId())
                .revieweeId(review.getRevieweeId())
                .score(review.getScore())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
