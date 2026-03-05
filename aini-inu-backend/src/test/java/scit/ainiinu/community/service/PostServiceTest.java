package scit.ainiinu.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.community.dto.CommentCreateRequest;
import scit.ainiinu.community.dto.CommentResponse;
import scit.ainiinu.community.dto.PostDetailResponse;
import scit.ainiinu.community.dto.PostLikeResponse;
import scit.ainiinu.community.dto.PostResponse;
import scit.ainiinu.community.dto.PostUpdateRequest;
import scit.ainiinu.community.entity.Comment;
import scit.ainiinu.community.entity.Post;
import scit.ainiinu.community.entity.PostLike;
import scit.ainiinu.community.exception.CommunityErrorCode;
import scit.ainiinu.community.repository.CommentRepository;
import scit.ainiinu.community.repository.PostLikeRepository;
import scit.ainiinu.community.repository.PostRepository;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        lenient().when(memberRepository.findAllById(anyIterable())).thenReturn(List.of());
    }

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("유효한 요청으로 댓글을 작성하면 성공하고 게시글의 댓글 수가 증가한다")
        void success() {
            // given
            Long postId = 1L;
            Long authorId = 1L;
            Post post = Post.create(authorId, "Content", Collections.emptyList());
            setId(post, postId);

            CommentCreateRequest request = new CommentCreateRequest();
            request.setContent("Nice dog!");

            Comment savedComment = Comment.create(post, authorId, "Nice dog!");
            setId(savedComment, 10L);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

            // when
            CommentResponse response = postService.createComment(authorId, postId, request);

            // then
            assertThat(response.getContent()).isEqualTo("Nice dog!");
            assertThat(response.getAuthor().getId()).isEqualTo(authorId);
            assertThat(response.getAuthor().getNickname()).isEqualTo("이웃");
            assertThat(post.getCommentCount()).isEqualTo(1);
            then(commentRepository).should(times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("작성자 회원 정보가 있으면 댓글 응답 author에 닉네임/프로필이 채워진다")
        void success_with_member_profile() {
            // given
            Long postId = 1L;
            Long authorId = 1L;
            Post post = Post.create(authorId, "Content", Collections.emptyList());
            setId(post, postId);

            CommentCreateRequest request = new CommentCreateRequest();
            request.setContent("Nice dog!");

            Comment savedComment = Comment.create(post, authorId, "Nice dog!");
            setId(savedComment, 10L);

            Member author = createMember(authorId, "몽이아빠", "https://cdn.example.com/profile.jpg");

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);
            given(memberRepository.findAllById(anyIterable())).willReturn(List.of(author));

            // when
            CommentResponse response = postService.createComment(authorId, postId, request);

            // then
            assertThat(response.getAuthor().getId()).isEqualTo(authorId);
            assertThat(response.getAuthor().getNickname()).isEqualTo("몽이아빠");
            assertThat(response.getAuthor().getProfileImageUrl()).isEqualTo("https://cdn.example.com/profile.jpg");
        }

        @Test
        @DisplayName("댓글 내용이 500자를 초과하면 예외가 발생한다 (CO005)")
        void fail_TooLongContent() {
            // given
            Long postId = 1L;
            Post post = Post.create(1L, "Content", Collections.emptyList());
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            CommentCreateRequest request = new CommentCreateRequest();
            // 501자 생성
            String tooLongContent = "a".repeat(501);
            request.setContent(tooLongContent);

            // when & then
            assertThatThrownBy(() -> postService.createComment(1L, postId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.INVALID_CONTENT_LENGTH);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("작성자가 본인의 댓글을 삭제하면 성공하고 댓글 수가 감소한다")
        void success() {
            // given
            Long postId = 1L;
            Long commentId = 10L;
            Long authorId = 1L;

            Post post = Post.create(authorId, "Content", Collections.emptyList());
            setId(post, postId);
            post.increaseComment();

            Comment comment = Comment.create(post, authorId, "Content");
            setId(comment, commentId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            postService.deleteComment(authorId, postId, commentId);

            // then
            assertThat(post.getCommentCount()).isEqualTo(0);
            then(commentRepository).should(times(1)).delete(comment);
        }

        @Test
        @DisplayName("게시글 작성자는 타인의 댓글도 삭제할 수 있다")
        void success_PostOwnerCanDelete() {
            // given
            Long postId = 1L;
            Long commentId = 10L;
            Long postAuthorId = 1L;
            Long commentAuthorId = 2L;

            Post post = Post.create(postAuthorId, "Content", Collections.emptyList());
            setId(post, postId);
            post.increaseComment();

            Comment comment = Comment.create(post, commentAuthorId, "Content");
            setId(comment, commentId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when
            postService.deleteComment(postAuthorId, postId, commentId);

            // then
            assertThat(post.getCommentCount()).isEqualTo(0);
            then(commentRepository).should(times(1)).delete(comment);
        }

        @Test
        @DisplayName("댓글 작성자도 게시글 작성자도 아닌 사용자가 삭제하려 하면 예외가 발생한다 (CO004)")
        void fail_NotOwner() {
            // given
            Long postId = 1L;
            Long commentId = 10L;
            Long postAuthorId = 1L;
            Long commentAuthorId = 2L;
            Long otherUserId = 3L;

            Post post = Post.create(postAuthorId, "Content", Collections.emptyList());
            setId(post, postId);

            Comment comment = Comment.create(post, commentAuthorId, "Content");
            setId(comment, commentId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> postService.deleteComment(otherUserId, postId, commentId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.NOT_COMMENT_OWNER);
        }

        @Test
        @DisplayName("댓글이 대상 게시글에 속하지 않으면 예외가 발생한다 (CO003)")
        void fail_CommentNotBelongToPost() {
            // given
            Long postId = 1L;
            Long commentId = 10L;
            Long authorId = 1L;

            Post post = Post.create(authorId, "Content", Collections.emptyList());
            setId(post, postId);

            Post anotherPost = Post.create(2L, "Another", Collections.emptyList());
            setId(anotherPost, 2L);

            Comment comment = Comment.create(anotherPost, 2L, "Content");
            setId(comment, commentId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> postService.deleteComment(authorId, postId, commentId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 삭제하려 하면 예외가 발생한다 (CO003)")
        void fail_CommentNotFound() {
            // given
            Long postId = 1L;
            Long commentId = 999L;
            Post post = Post.create(1L, "Content", Collections.emptyList());
            
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.deleteComment(1L, postId, commentId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("좋아요 토글")
    class ToggleLike {

        @Test
        @DisplayName("좋아요가 없을 때 요청하면 생성되고 카운트가 증가한다")
        void createLike() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Post post = Post.create(memberId, "Content", Collections.emptyList());
            setId(post, postId);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(postLikeRepository.findByPostAndMemberId(post, memberId)).willReturn(Optional.empty());

            // when
            PostLikeResponse response = postService.toggleLike(memberId, postId);

            // then
            assertThat(response.isLiked()).isTrue();
            assertThat(response.getLikeCount()).isEqualTo(1);
            then(postLikeRepository).should(times(1)).save(any(PostLike.class));
        }

        @Test
        @DisplayName("좋아요가 이미 있을 때 요청하면 삭제되고 카운트가 감소한다")
        void removeLike() {
            // given
            Long postId = 1L;
            Long memberId = 1L;
            Post post = Post.create(memberId, "Content", Collections.emptyList());
            setId(post, postId);
            post.increaseLike(); // 기존 좋아요 상태 반영 (count=1)

            PostLike existingLike = PostLike.create(post, memberId);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(postLikeRepository.findByPostAndMemberId(post, memberId)).willReturn(Optional.of(existingLike));

            // when
            PostLikeResponse response = postService.toggleLike(memberId, postId);

            // then
            assertThat(response.isLiked()).isFalse();
            assertThat(response.getLikeCount()).isEqualTo(0);
            then(postLikeRepository).should(times(1)).delete(existingLike);
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {
        
        @Test
        @DisplayName("작성자가 본인의 게시글을 수정하면 성공한다")
        void success() {
            // given
            Long postId = 1L;
            Long authorId = 1L;
            Post post = Post.create(authorId, "Original Content", Collections.emptyList());
            setId(post, postId);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            
            PostUpdateRequest request = new PostUpdateRequest();
            request.setContent("Updated Content");
            request.setImageUrls(List.of("new.jpg"));

            // when
            PostResponse response = postService.updatePost(authorId, postId, request);

            // then
            assertThat(response.getContent()).isEqualTo("Updated Content");
            assertThat(response.getAuthor().getId()).isEqualTo(authorId);
            assertThat(response.getAuthor().getNickname()).isEqualTo("이웃");
        }

        @Test
        @DisplayName("content가 비어있고 caption만 있어도 수정에 성공한다")
        void success_with_caption_alias() {
            // given
            Long postId = 1L;
            Long authorId = 1L;
            Post post = Post.create(authorId, "Original Content", Collections.emptyList());
            setId(post, postId);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            PostUpdateRequest request = new PostUpdateRequest();
            request.setCaption("Updated By Caption");

            // when
            PostResponse response = postService.updatePost(authorId, postId, request);

            // then
            assertThat(response.getContent()).isEqualTo("Updated By Caption");
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정하려 하면 예외가 발생한다 (CO002)")
        void fail_NotOwner() {
            // given
            Long postId = 1L;
            Long authorId = 1L;
            Long otherUserId = 2L;
            Post post = Post.create(authorId, "Content", Collections.emptyList());
            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            PostUpdateRequest request = new PostUpdateRequest();

            // when & then
            assertThatThrownBy(() -> postService.updatePost(otherUserId, postId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.NOT_POST_OWNER);
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("작성자가 본인의 게시글을 삭제하면 성공한다")
        void success() {
            // given
            Long postId = 1L;
            Long authorId = 1L;
            Post post = Post.create(authorId, "삭제 대상", Collections.emptyList());
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            postService.deletePost(authorId, postId);

            // then
            then(postRepository).should().delete(post);
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 삭제하려 하면 예외가 발생한다 (CO002)")
        void fail_notOwner() {
            // given
            Long postId = 1L;
            Post post = Post.create(1L, "삭제 대상", Collections.emptyList());
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.deletePost(2L, postId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.NOT_POST_OWNER);
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("게시글 댓글 목록을 SliceResponse로 반환한다")
        void success() {
            // given
            Long memberId = 1L;
            Long postId = 100L;
            Pageable pageable = PageRequest.of(0, 20);

            Post post = Post.create(1L, "Post Content", Collections.emptyList());
            setId(post, postId);

            Comment comment1 = Comment.create(post, 2L, "Comment 1");
            setId(comment1, 1L);
            Comment comment2 = Comment.create(post, 3L, "Comment 2");
            setId(comment2, 2L);

            Slice<Comment> commentSlice = new SliceImpl<>(List.of(comment1, comment2), pageable, false);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findByPostOrderByCreatedAtAsc(post, pageable)).willReturn(commentSlice);

            // when
            SliceResponse<CommentResponse> response = postService.getComments(memberId, postId, pageable);

            // then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getContent().get(0).getContent()).isEqualTo("Comment 1");
            assertThat(response.getContent().get(0).getAuthor().getId()).isEqualTo(2L);
            assertThat(response.getContent().get(0).getAuthor().getNickname()).isEqualTo("이웃");
            then(commentRepository).should(times(1)).findByPostOrderByCreatedAtAsc(post, pageable);
        }
    }

    @Nested
    @DisplayName("게시글 목록 조회")
    class GetPosts {

        @Test
        @DisplayName("게시글 작성자 회원 정보가 없으면 author는 fallback으로 채워진다")
        void success_with_fallback_author() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            Post post = Post.create(2L, "Post Content", Collections.emptyList());
            setId(post, 100L);

            Slice<Post> postSlice = new SliceImpl<>(List.of(post), pageable, false);

            given(postRepository.findAllBy(pageable)).willReturn(postSlice);
            given(postLikeRepository.existsByPostAndMemberId(post, memberId)).willReturn(false);

            // when
            SliceResponse<PostResponse> response = postService.getPosts(memberId, pageable);

            // then
            assertThat(response.getContent()).hasSize(1);
            PostResponse item = response.getContent().get(0);
            assertThat(item.getAuthor().getId()).isEqualTo(2L);
            assertThat(item.getAuthor().getNickname()).isEqualTo("이웃");
            assertThat(item.isLiked()).isFalse();
        }

        @Test
        @DisplayName("게시글 작성자 회원 정보가 있으면 author 닉네임/프로필이 채워진다")
        void success_with_member_profile() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            Post post = Post.create(2L, "Post Content", Collections.emptyList());
            setId(post, 100L);

            Slice<Post> postSlice = new SliceImpl<>(List.of(post), pageable, false);
            Member postAuthor = createMember(2L, "몽이아빠", "https://cdn.example.com/profile.jpg");

            given(postRepository.findAllBy(pageable)).willReturn(postSlice);
            given(postLikeRepository.existsByPostAndMemberId(post, memberId)).willReturn(true);
            given(memberRepository.findAllById(anyIterable())).willReturn(List.of(postAuthor));

            // when
            SliceResponse<PostResponse> response = postService.getPosts(memberId, pageable);

            // then
            PostResponse item = response.getContent().get(0);
            assertThat(item.getAuthor().getNickname()).isEqualTo("몽이아빠");
            assertThat(item.getAuthor().getProfileImageUrl()).isEqualTo("https://cdn.example.com/profile.jpg");
            assertThat(item.isLiked()).isTrue();
        }
    }

    @Nested
    @DisplayName("게시글 상세 조회")
    class GetPostDetail {

        @Test
        @DisplayName("게시글 ID로 상세 정보를 조회하면 댓글 목록과 함께 반환한다")
        void success() {
            // given
            Long postId = 100L;
            Post post = Post.create(1L, "Post Content", Collections.emptyList());
            setId(post, postId);

            Comment comment1 = Comment.create(post, 2L, "Comment 1");
            setId(comment1, 1L);
            Comment comment2 = Comment.create(post, 3L, "Comment 2");
            setId(comment2, 2L);

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findAllByPostOrderByCreatedAtAsc(post))
                    .willReturn(List.of(comment1, comment2));

            // when
            Long memberId = 1L; // 테스트용 memberId
            PostDetailResponse response = postService.getPostDetail(memberId, postId);

            // then
            assertThat(response.getId()).isEqualTo(postId);
            assertThat(response.getAuthor().getId()).isEqualTo(1L);
            assertThat(response.getAuthor().getNickname()).isEqualTo("이웃");
            assertThat(response.getComments()).hasSize(2);
            assertThat(response.getComments().get(0).getAuthor().getNickname()).isEqualTo("이웃");
            then(commentRepository).should(times(1)).findAllByPostOrderByCreatedAtAsc(post);
        }

        @Test
        @DisplayName("작성자 회원 정보가 있으면 게시글/댓글 author에 닉네임/프로필이 채워진다")
        void success_with_member_profile() {
            // given
            Long postId = 100L;
            Post post = Post.create(1L, "Post Content", Collections.emptyList());
            setId(post, postId);

            Comment comment = Comment.create(post, 2L, "Comment 1");
            setId(comment, 1L);

            Member postAuthor = createMember(1L, "몽이아빠", "https://cdn.example.com/post-author.jpg");
            Member commentAuthor = createMember(2L, "보리누나", "https://cdn.example.com/comment-author.jpg");

            given(postRepository.findById(postId)).willReturn(Optional.of(post));
            given(commentRepository.findAllByPostOrderByCreatedAtAsc(post)).willReturn(List.of(comment));
            given(memberRepository.findAllById(anyIterable())).willReturn(List.of(postAuthor, commentAuthor));

            // when
            PostDetailResponse response = postService.getPostDetail(1L, postId);

            // then
            assertThat(response.getAuthor().getNickname()).isEqualTo("몽이아빠");
            assertThat(response.getAuthor().getProfileImageUrl()).isEqualTo("https://cdn.example.com/post-author.jpg");
            assertThat(response.getComments().get(0).getAuthor().getNickname()).isEqualTo("보리누나");
            assertThat(response.getComments().get(0).getAuthor().getProfileImageUrl())
                    .isEqualTo("https://cdn.example.com/comment-author.jpg");
        }
    }
    
    // ... (기타 테스트 및 헬퍼 메서드는 기존과 동일) ...
    private Member createMember(Long memberId, String nickname, String profileImageUrl) {
        Member member = Member.builder()
                .email("member-" + memberId + "@test.com")
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
        setId(member, memberId);
        return member;
    }

    // ID 설정을 위한 리플렉션 헬퍼 메서드
    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Test setup failed: cannot set ID", e);
        }
    }
}
