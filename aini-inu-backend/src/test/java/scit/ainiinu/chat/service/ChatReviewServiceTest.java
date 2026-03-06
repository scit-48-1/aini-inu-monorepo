package scit.ainiinu.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.dto.request.ChatReviewCreateRequest;
import scit.ainiinu.chat.dto.response.ChatReviewResponse;
import scit.ainiinu.chat.dto.response.MyChatReviewResponse;
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.common.response.SliceResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatReviewServiceTest {

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatReviewRepository chatReviewRepository;

    @InjectMocks
    private ChatReviewService chatReviewService;

    @Nested
    @DisplayName("createReview")
    class CreateReview {

        @Test
        @DisplayName("정상 리뷰 생성 시 ChatReviewResponse를 반환한다")
        void success() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            ChatReviewCreateRequest request = new ChatReviewCreateRequest();
            request.setRevieweeId(2L);
            request.setScore(4);
            request.setComment("좋은 산책이었어요");

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, 2L)).willReturn(true);
            given(chatReviewRepository.existsByChatRoomIdAndReviewerIdAndRevieweeId(chatRoomId, memberId, 2L)).willReturn(false);

            ChatReview saved = ChatReview.create(chatRoomId, memberId, 2L, 4, "좋은 산책이었어요");
            ReflectionTestUtils.setField(saved, "id", 100L);
            given(chatReviewRepository.save(any(ChatReview.class))).willReturn(saved);

            // when
            ChatReviewResponse response = chatReviewService.createReview(memberId, chatRoomId, request);

            // then
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getScore()).isEqualTo(4);
            assertThat(response.getReviewerId()).isEqualTo(memberId);
            assertThat(response.getRevieweeId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("이미 리뷰가 존재하면 REVIEW_ALREADY_EXISTS 예외")
        void duplicate_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            ChatReviewCreateRequest request = new ChatReviewCreateRequest();
            request.setRevieweeId(2L);
            request.setScore(3);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, 2L)).willReturn(true);
            given(chatReviewRepository.existsByChatRoomIdAndReviewerIdAndRevieweeId(chatRoomId, memberId, 2L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatReviewService.createReview(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.REVIEW_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("참여자가 아니면 ROOM_ACCESS_DENIED 예외")
        void nonParticipant_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            ChatReviewCreateRequest request = new ChatReviewCreateRequest();
            request.setRevieweeId(2L);
            request.setScore(3);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatReviewService.createReview(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("자기 자신을 리뷰하면 INVALID_REQUEST 예외")
        void selfReview_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            ChatReviewCreateRequest request = new ChatReviewCreateRequest();
            request.setRevieweeId(1L);
            request.setScore(5);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatReviewService.createReview(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("getMyReview")
    class GetMyReview {

        @Test
        @DisplayName("리뷰가 존재하면 exists=true와 함께 반환")
        void exists_returnsReview() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            ChatReview review = ChatReview.create(chatRoomId, memberId, 2L, 5, "최고");
            ReflectionTestUtils.setField(review, "id", 50L);
            given(chatReviewRepository.findTopByChatRoomIdAndReviewerIdOrderByCreatedAtDesc(chatRoomId, memberId))
                    .willReturn(Optional.of(review));

            // when
            MyChatReviewResponse response = chatReviewService.getMyReview(memberId, chatRoomId);

            // then
            assertThat(response.isExists()).isTrue();
            assertThat(response.getReview()).isNotNull();
            assertThat(response.getReview().getScore()).isEqualTo(5);
        }

        @Test
        @DisplayName("리뷰가 없으면 exists=false 반환")
        void notExists_returnsFalse() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);
            given(chatReviewRepository.findTopByChatRoomIdAndReviewerIdOrderByCreatedAtDesc(chatRoomId, memberId))
                    .willReturn(Optional.empty());

            // when
            MyChatReviewResponse response = chatReviewService.getMyReview(memberId, chatRoomId);

            // then
            assertThat(response.isExists()).isFalse();
            assertThat(response.getReview()).isNull();
        }
    }

    @Nested
    @DisplayName("getReviews")
    class GetReviews {

        @Test
        @DisplayName("리뷰 목록을 페이지네이션하여 반환한다")
        void returnsSlice() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            Pageable pageable = PageRequest.of(0, 10);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            ChatReview review1 = ChatReview.create(chatRoomId, 2L, 3L, 4, "좋아요");
            ReflectionTestUtils.setField(review1, "id", 1L);
            ChatReview review2 = ChatReview.create(chatRoomId, 3L, 2L, 5, "최고");
            ReflectionTestUtils.setField(review2, "id", 2L);

            given(chatReviewRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(chatRoomId, pageable))
                    .willReturn(new SliceImpl<>(List.of(review1, review2), pageable, false));

            // when
            SliceResponse<ChatReviewResponse> response = chatReviewService.getReviews(memberId, chatRoomId, pageable);

            // then
            assertThat(response.getContent()).hasSize(2);
        }
    }
}
