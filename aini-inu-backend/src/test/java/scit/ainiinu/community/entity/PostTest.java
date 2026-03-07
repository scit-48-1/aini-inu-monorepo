package scit.ainiinu.community.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import scit.ainiinu.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Post 엔티티")
class PostTest {

    @Nested
    @DisplayName("생성")
    class Create {
        @Test
        @DisplayName("유효한 정보로 생성하면 성공한다")
        void create_success() {
            // when
            Post post = Post.create(1L, "hello", List.of("a.jpg", "b.jpg"));

            // then
            assertThat(post.getAuthorId()).isEqualTo(1L);
            assertThat(post.getContent()).isEqualTo("hello");
            assertThat(post.getImageUrls()).hasSize(2);
            assertThat(post.getLikeCount()).isZero();
            assertThat(post.getCommentCount()).isZero();
        }

        @Test
        @DisplayName("내용이 공백이면 예외가 발생한다")
        void create_fail_when_content_blank() {
            // when & then
            assertThatThrownBy(() -> Post.create(1L, "   ", List.of()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("이미지가 5개를 초과하면 예외가 발생한다")
        void create_fail_when_images_over_5() {
            // given
            List<String> images = List.of("1", "2", "3", "4", "5", "6");

            // when & then
            assertThatThrownBy(() -> Post.create(1L, "ok", images))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("좋아요 카운트")
    class LikeCount {
        @Test
        @DisplayName("좋아요 수는 0 미만으로 내려가지 않는다")
        void like_never_below_zero() {
            // given
            Post post = Post.create(1L, "ok", List.of());

            // when
            post.decreaseLike();

            // then
            assertThat(post.getLikeCount()).isZero();
        }

        @Test
        @DisplayName("좋아요 증가 후 감소하면 정상적으로 동작한다")
        void increase_then_decrease() {
            // given
            Post post = Post.create(1L, "ok", List.of());

            // when
            post.increaseLike();
            post.decreaseLike();
            post.decreaseLike();

            // then
            assertThat(post.getLikeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("댓글 카운트")
    class CommentCount {
        @Test
        @DisplayName("댓글 수는 0 미만으로 내려가지 않는다")
        void comment_never_below_zero() {
            // given
            Post post = Post.create(1L, "ok", List.of());

            // when
            post.increaseComment();
            post.increaseComment();
            post.decreaseComment();
            post.decreaseComment();
            post.decreaseComment();

            // then
            assertThat(post.getCommentCount()).isZero();
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("유효한 내용으로 수정하면 성공한다")
        void update_success() {
            // given
            Post post = Post.create(1L, "원래 내용", List.of("old.jpg"));

            // when
            post.update("수정된 내용", List.of("new.jpg"));

            // then
            assertThat(post.getContent()).isEqualTo("수정된 내용");
            assertThat(post.getImageUrls()).containsExactly("new.jpg");
        }

        @Test
        @DisplayName("내용이 2000자를 초과하면 예외가 발생한다")
        void update_fail_when_content_too_long() {
            // given
            Post post = Post.create(1L, "ok", List.of());
            String tooLong = "a".repeat(2001);

            // when & then
            assertThatThrownBy(() -> post.update(tooLong, List.of()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("이미지가 5개를 초과하면 예외가 발생한다")
        void update_fail_when_images_over_5() {
            // given
            Post post = Post.create(1L, "ok", List.of());
            List<String> images = List.of("1", "2", "3", "4", "5", "6");

            // when & then
            assertThatThrownBy(() -> post.update("ok", images))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("내용이 공백이면 예외가 발생한다")
        void update_fail_when_content_blank() {
            // given
            Post post = Post.create(1L, "ok", List.of());

            // when & then
            assertThatThrownBy(() -> post.update("   ", List.of()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("이미지 null이면 빈 리스트로 처리된다")
        void update_success_with_null_images() {
            // given
            Post post = Post.create(1L, "ok", List.of("old.jpg"));

            // when
            post.update("수정됨", null);

            // then
            assertThat(post.getContent()).isEqualTo("수정됨");
            assertThat(post.getImageUrls()).isEmpty();
        }
    }

    @Nested
    @DisplayName("버전 초기화")
    class Version {

        @Test
        @DisplayName("생성 시 version은 0L로 초기화된다")
        void version_initialized_to_zero() {
            // when
            Post post = Post.create(1L, "ok", List.of());

            // then
            assertThat(post.getVersion()).isEqualTo(0L);
        }
    }
}
