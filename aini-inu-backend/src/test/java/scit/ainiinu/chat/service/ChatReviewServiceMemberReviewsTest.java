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
import scit.ainiinu.chat.dto.response.MemberReviewSummaryResponse;
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatReviewServiceMemberReviewsTest {

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatReviewRepository chatReviewRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ChatReviewService chatReviewService;

    @Nested
    @DisplayName("getMemberReviews")
    class GetMemberReviews {

        @Test
        @DisplayName("리뷰 목록과 요약 정보를 정상 매핑하여 반환한다")
        void returnsReviewsWithSummary() {
            // given
            Long revieweeId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatReview review1 = ChatReview.create(10L, 2L, revieweeId, 5, "최고");
            ReflectionTestUtils.setField(review1, "id", 100L);
            ChatReview review2 = ChatReview.create(11L, 3L, revieweeId, 4, "좋아요");
            ReflectionTestUtils.setField(review2, "id", 101L);

            given(chatReviewRepository.findByRevieweeIdOrderByCreatedAtDescIdDesc(revieweeId, pageable))
                    .willReturn(new SliceImpl<>(List.of(review1, review2), pageable, false));

            Member reviewer1 = Member.builder().email("a@test.com").password("pw").nickname("리뷰어1").memberType(MemberType.PET_OWNER).build();
            ReflectionTestUtils.setField(reviewer1, "id", 2L);
            Member reviewer2 = Member.builder().email("b@test.com").password("pw").nickname("리뷰어2").memberType(MemberType.PET_OWNER).build();
            ReflectionTestUtils.setField(reviewer2, "id", 3L);
            given(memberRepository.findAllById(List.of(2L, 3L))).willReturn(List.of(reviewer1, reviewer2));

            given(chatReviewRepository.countByRevieweeId(revieweeId)).willReturn(2L);
            given(chatReviewRepository.findAverageScoreByRevieweeId(revieweeId)).willReturn(4.5);
            given(chatReviewRepository.findScoreDistributionByRevieweeId(revieweeId))
                    .willReturn(List.<Object[]>of(new Object[]{4, 1L}, new Object[]{5, 1L}));

            // when
            MemberReviewSummaryResponse response = chatReviewService.getMemberReviews(revieweeId, pageable);

            // then
            assertThat(response.getTotalCount()).isEqualTo(2);
            assertThat(response.getAverageScore()).isEqualTo(4.5);
            assertThat(response.getReviews().getContent()).hasSize(2);
            assertThat(response.getReviews().getContent().get(0).getReviewerNickname()).isEqualTo("리뷰어1");
            assertThat(response.getReviews().getContent().get(1).getReviewerNickname()).isEqualTo("리뷰어2");
            assertThat(response.getScoreDistribution().get(5)).isEqualTo(1L);
            assertThat(response.getScoreDistribution().get(4)).isEqualTo(1L);
            assertThat(response.getScoreDistribution().get(3)).isEqualTo(0L);
        }

        @Test
        @DisplayName("리뷰가 없으면 빈 결과와 기본값을 반환한다")
        void emptyReviews_returnsDefaults() {
            // given
            Long revieweeId = 99L;
            Pageable pageable = PageRequest.of(0, 20);

            given(chatReviewRepository.findByRevieweeIdOrderByCreatedAtDescIdDesc(revieweeId, pageable))
                    .willReturn(new SliceImpl<>(Collections.emptyList(), pageable, false));
            given(memberRepository.findAllById(Collections.emptyList())).willReturn(Collections.emptyList());
            given(chatReviewRepository.countByRevieweeId(revieweeId)).willReturn(0L);
            given(chatReviewRepository.findAverageScoreByRevieweeId(revieweeId)).willReturn(0.0);
            given(chatReviewRepository.findScoreDistributionByRevieweeId(revieweeId)).willReturn(Collections.emptyList());

            // when
            MemberReviewSummaryResponse response = chatReviewService.getMemberReviews(revieweeId, pageable);

            // then
            assertThat(response.getTotalCount()).isZero();
            assertThat(response.getAverageScore()).isZero();
            assertThat(response.getReviews().getContent()).isEmpty();
            assertThat(response.getScoreDistribution()).containsEntry(1, 0L);
            assertThat(response.getScoreDistribution()).containsEntry(5, 0L);
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext가 true이다")
        void pagination_hasNext() {
            // given
            Long revieweeId = 1L;
            Pageable pageable = PageRequest.of(0, 1);

            ChatReview review = ChatReview.create(10L, 2L, revieweeId, 3, "보통");
            ReflectionTestUtils.setField(review, "id", 200L);

            given(chatReviewRepository.findByRevieweeIdOrderByCreatedAtDescIdDesc(revieweeId, pageable))
                    .willReturn(new SliceImpl<>(List.of(review), pageable, true));

            Member reviewer = Member.builder().email("c@test.com").password("pw").nickname("리뷰어").memberType(MemberType.PET_OWNER).build();
            ReflectionTestUtils.setField(reviewer, "id", 2L);
            given(memberRepository.findAllById(List.of(2L))).willReturn(List.of(reviewer));

            given(chatReviewRepository.countByRevieweeId(revieweeId)).willReturn(5L);
            given(chatReviewRepository.findAverageScoreByRevieweeId(revieweeId)).willReturn(3.0);
            given(chatReviewRepository.findScoreDistributionByRevieweeId(revieweeId))
                    .willReturn(List.<Object[]>of(new Object[]{3, 5L}));

            // when
            MemberReviewSummaryResponse response = chatReviewService.getMemberReviews(revieweeId, pageable);

            // then
            assertThat(response.getReviews().isHasNext()).isTrue();
            assertThat(response.getReviews().getContent()).hasSize(1);
            assertThat(response.getTotalCount()).isEqualTo(5);
        }
    }
}
