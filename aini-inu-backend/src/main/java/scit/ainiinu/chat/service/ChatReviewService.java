package scit.ainiinu.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.dto.request.ChatReviewCreateRequest;
import scit.ainiinu.chat.dto.response.ChatReviewResponse;
import scit.ainiinu.chat.dto.response.MemberReviewResponse;
import scit.ainiinu.chat.dto.response.MemberReviewSummaryResponse;
import scit.ainiinu.chat.dto.response.MyChatReviewResponse;
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReviewService {

    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatReviewRepository chatReviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ChatReviewResponse createReview(Long memberId, Long chatRoomId, ChatReviewCreateRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));

        if (chatRoom.getOrigin() != ChatRoomOrigin.WALK || !chatRoom.getWalkConfirmed()) {
            throw new ChatException(ChatErrorCode.WALK_NOT_COMPLETED);
        }

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

    public MemberReviewSummaryResponse getMemberReviews(Long revieweeId, Pageable pageable) {
        Slice<ChatReview> reviewSlice = chatReviewRepository.findByRevieweeIdOrderByCreatedAtDescIdDesc(revieweeId, pageable);

        List<Long> reviewerIds = reviewSlice.getContent().stream()
                .map(ChatReview::getReviewerId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Member> memberMap = memberRepository.findAllById(reviewerIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        Slice<MemberReviewResponse> mapped = reviewSlice.map(review -> {
            Member reviewer = memberMap.get(review.getReviewerId());
            return MemberReviewResponse.builder()
                    .id(review.getId())
                    .reviewerId(review.getReviewerId())
                    .reviewerNickname(reviewer != null ? reviewer.getNickname() : null)
                    .reviewerProfileImageUrl(reviewer != null ? reviewer.getProfileImageUrl() : null)
                    .score(review.getScore())
                    .comment(review.getComment())
                    .createdAt(review.getCreatedAt())
                    .build();
        });

        long totalCount = chatReviewRepository.countByRevieweeId(revieweeId);
        double averageScore = chatReviewRepository.findAverageScoreByRevieweeId(revieweeId);

        Map<Integer, Long> scoreDistribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            scoreDistribution.put(i, 0L);
        }
        for (Object[] row : chatReviewRepository.findScoreDistributionByRevieweeId(revieweeId)) {
            scoreDistribution.put((Integer) row[0], (Long) row[1]);
        }

        return MemberReviewSummaryResponse.builder()
                .averageScore(Math.round(averageScore * 10) / 10.0)
                .totalCount(totalCount)
                .scoreDistribution(scoreDistribution)
                .reviews(SliceResponse.of(mapped))
                .build();
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
