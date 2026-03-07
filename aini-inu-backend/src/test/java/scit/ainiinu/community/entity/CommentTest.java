package scit.ainiinu.community.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scit.ainiinu.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Comment 엔티티")
class CommentTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 정보로 생성하면 성공한다")
        void create_success() {
            // when
            Comment comment = Comment.create(1L, 2L, "좋은 게시글이네요!");

            // then
            assertThat(comment.getPostId()).isEqualTo(1L);
            assertThat(comment.getAuthorId()).isEqualTo(2L);
            assertThat(comment.getContent()).isEqualTo("좋은 게시글이네요!");
        }

        @Test
        @DisplayName("내용이 null이면 예외가 발생한다")
        void create_fail_when_content_null() {
            // when & then
            assertThatThrownBy(() -> Comment.create(1L, 2L, null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("내용이 500자를 초과하면 예외가 발생한다")
        void create_fail_when_content_exceeds_500() {
            // given
            String tooLong = "a".repeat(501);

            // when & then
            assertThatThrownBy(() -> Comment.create(1L, 2L, tooLong))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("내용이 정확히 500자이면 성공한다")
        void create_success_at_boundary_500() {
            // given
            String exactly500 = "a".repeat(500);

            // when
            Comment comment = Comment.create(1L, 2L, exactly500);

            // then
            assertThat(comment.getContent()).hasSize(500);
        }
    }
}
