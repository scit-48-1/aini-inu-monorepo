package scit.ainiinu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.community.dto.CommentCreateRequest;
import scit.ainiinu.community.dto.CommentResponse;
import scit.ainiinu.community.dto.PostCreateRequest;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private static final String UNKNOWN_AUTHOR_NICKNAME = "이웃";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;

    /**
     * 게시글 목록 조회 (무한 스크롤)
     * @param memberId 현재 로그인 사용자 ID (좋아요 여부 확인용)
     */
    public SliceResponse<PostResponse> getPosts(Long memberId, Pageable pageable) {
        Slice<Post> posts = postRepository.findAllBy(pageable);
        Map<Long, Member> memberMap = loadMemberMap(
                posts.getContent().stream()
                        .map(Post::getAuthorId)
                        .toList()
        );

        return SliceResponse.of(posts.map(post -> {
            boolean isLiked = postLikeRepository.existsByPostAndMemberId(post, memberId);
            return PostResponse.from(post, resolveAuthor(post.getAuthorId(), memberMap), isLiked);
        }));
    }

    /**
     * 게시글 상세 조회 (댓글 포함)
     * - 게시글 내용과 해당 게시글에 달린 댓글 목록을 반환합니다.
     * @param memberId 현재 로그인 사용자 ID (좋아요 여부 확인용)
     */
    public PostDetailResponse getPostDetail(Long memberId, Long postId) {
        // 1. 게시글 조회 (없으면 CO001 예외)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        // 2. 댓글 목록 조회
        List<Comment> comments = commentRepository.findAllByPostOrderByCreatedAtAsc(post);
        Map<Long, Member> memberMap = loadMemberMap(
                Stream.concat(
                                Stream.of(post.getAuthorId()),
                                comments.stream().map(Comment::getAuthorId)
                        )
                        .toList()
        );

        // 3. 댓글 응답 변환
        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        resolveAuthor(comment.getAuthorId(), memberMap)
                ))
                .toList();

        // 4. 좋아요 여부 조회
        boolean isLiked = postLikeRepository.existsByPostAndMemberId(post, memberId);

        return PostDetailResponse.of(
                post,
                resolveAuthor(post.getAuthorId(), memberMap),
                commentResponses,
                isLiked
        );
    }

    /**
     * 댓글 목록 조회 (무한 스크롤)
     */
    public SliceResponse<CommentResponse> getComments(Long memberId, Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        Slice<Comment> comments = commentRepository.findByPostOrderByCreatedAtAsc(post, pageable);
        Map<Long, Member> memberMap = loadMemberMap(
                comments.getContent().stream()
                        .map(Comment::getAuthorId)
                        .toList()
        );

        return SliceResponse.of(comments.map(comment -> CommentResponse.from(
                comment,
                resolveAuthor(comment.getAuthorId(), memberMap)
        )));
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest request) {
        Post post = Post.create(
                memberId,
                request.getResolvedContent(),
                request.getImageUrls()
        );
        Post saved = postRepository.save(post);
        Map<Long, Member> memberMap = loadMemberMap(List.of(saved.getAuthorId()));
        return PostResponse.from(saved, resolveAuthor(saved.getAuthorId(), memberMap), false);
    }

    /**
     * 게시글 수정
     * - 작성자 본인만 수정 가능합니다. (CO002)
     */
    @Transactional
    public PostResponse updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        // 2. 작성자 본인 확인
        if (!post.getAuthorId().equals(memberId)) {
            throw new BusinessException(CommunityErrorCode.NOT_POST_OWNER);
        }

        // 3. 내용 및 이미지 수정 (길이 검증 등은 Entity 내부에서 처리)
        post.update(request.getResolvedContent(), request.getImageUrls());

        // 4. 좋아요 여부 조회 (수정한 사용자는 작성자이므로 memberId를 사용)
        boolean isLiked = postLikeRepository.existsByPostAndMemberId(post, memberId);
        Map<Long, Member> memberMap = loadMemberMap(List.of(post.getAuthorId()));

        return PostResponse.from(post, resolveAuthor(post.getAuthorId(), memberMap), isLiked);
    }

    /**
     * 게시글 삭제
     * - 작성자 본인만 삭제 가능합니다. (CO002)
     */
    @Transactional
    public void deletePost(Long memberId, Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        // 2. 작성자 본인 확인
        if (!post.getAuthorId().equals(memberId)) {
            throw new BusinessException(CommunityErrorCode.NOT_POST_OWNER);
        }

        // 3. 게시글 삭제
        postRepository.delete(post);
    }

    /**
     * 좋아요 토글 (생성/취소)
     * - 이미 좋아요를 누른 경우 -> 취소 (카운트 감소)
     * - 누르지 않은 경우 -> 생성 (카운트 증가)
     */
    @Transactional
    public PostLikeResponse toggleLike(Long memberId, Long postId) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        // 2. 좋아요 존재 여부 확인
        Optional<PostLike> existingLike = postLikeRepository.findByPostAndMemberId(post, memberId);

        boolean isLiked;
        
        if (existingLike.isPresent()) {
            // 3. 이미 존재하면 삭제 (좋아요 취소) 및 카운트 감소
            postLikeRepository.delete(existingLike.get());
            post.decreaseLike();
            isLiked = false;
        } else {
            // 4. 없으면 생성 (좋아요) 및 카운트 증가
            postLikeRepository.save(PostLike.create(post, memberId));
            post.increaseLike();
            isLiked = true;
        }

        return new PostLikeResponse(isLiked, post.getLikeCount());
    }

    /**
     * 댓글 작성
     * - 댓글 작성 시 게시글의 댓글 수가 1 증가합니다.
     * - 내용은 최대 500자입니다. (CO005)
     */
    @Transactional
    public CommentResponse createComment(Long memberId, Long postId, CommentCreateRequest request) {
        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        // 2. 댓글 엔티티 생성 및 저장 (길이 검증은 Entity 내부에서 수행)
        Comment comment = Comment.create(post, memberId, request.getContent());
        Comment savedComment = commentRepository.save(comment);

        // 3. 게시글 댓글 수 증가
        post.increaseComment();

        Map<Long, Member> memberMap = loadMemberMap(List.of(savedComment.getAuthorId()));
        return CommentResponse.from(savedComment, resolveAuthor(savedComment.getAuthorId(), memberMap));
    }

    /**
     * 댓글 삭제
     * - 댓글 작성자 또는 게시글 작성자만 삭제 가능합니다. (CO004)
     * - 삭제 시 게시글의 댓글 수가 1 감소합니다.
     */
    @Transactional
    public void deleteComment(Long memberId, Long postId, Long commentId) {
        // 1. 게시글 조회 (댓글 수 감소를 위해 필요)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

        // 2. 댓글 조회 (없으면 CO003)
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND));

        // 3. 댓글-게시글 매칭 검증 (다른 게시글의 댓글 ID 오용 방지)
        if (!comment.getPost().getId().equals(postId)) {
            throw new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND);
        }

        // 4. 삭제 권한 확인: 댓글 작성자 또는 게시글 작성자
        boolean isCommentOwner = comment.getAuthorId().equals(memberId);
        boolean isPostOwner = post.getAuthorId().equals(memberId);
        if (!isCommentOwner && !isPostOwner) {
            throw new BusinessException(CommunityErrorCode.NOT_COMMENT_OWNER);
        }

        // 5. 댓글 삭제 및 카운트 감소
        commentRepository.delete(comment);
        post.decreaseComment();
    }

    private Map<Long, Member> loadMemberMap(Collection<Long> memberIds) {
        List<Long> distinctIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }

        return memberRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }

    private PostResponse.Author resolveAuthor(Long memberId, Map<Long, Member> memberMap) {
        Member member = memberMap.get(memberId);
        if (member == null) {
            return PostResponse.Author.of(memberId, UNKNOWN_AUTHOR_NICKNAME, null);
        }
        return PostResponse.Author.of(member.getId(), member.getNickname(), member.getProfileImageUrl());
    }
}
