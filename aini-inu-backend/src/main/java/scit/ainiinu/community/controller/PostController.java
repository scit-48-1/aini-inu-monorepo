package scit.ainiinu.community.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.community.dto.CommentCreateRequest;
import scit.ainiinu.community.dto.CommentResponse;
import scit.ainiinu.community.dto.PostCreateRequest;
import scit.ainiinu.community.dto.PostDetailResponse;
import scit.ainiinu.community.dto.PostLikeResponse;
import scit.ainiinu.community.dto.PostResponse;
import scit.ainiinu.community.dto.PostUpdateRequest;
import scit.ainiinu.community.service.PostService;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Community", description = "커뮤니티 API")
@SecurityRequirement(name = "bearerAuth")
public class PostController {

    private final PostService postService;

    // 게시글 목록 조회 (무한 스크롤)
    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "무한 스크롤 기반으로 게시글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<PostResponse>>> getPosts(
            @CurrentMember Long memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SliceResponse<PostResponse> response = postService.getPosts(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 단건 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId
    ) {
        PostDetailResponse response = postService.getPostDetail(memberId, postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 댓글 목록 조회
    @GetMapping("/{postId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "게시글 댓글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<CommentResponse>>> getComments(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        SliceResponse<CommentResponse> response = postService.getComments(memberId, postId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 게시글 수정
    @PatchMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "게시글 작성자가 내용을 수정합니다.")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId,
            @RequestBody @Valid PostUpdateRequest request
    ) {
        PostResponse response = postService.updatePost(memberId, postId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "게시글 작성자가 게시글을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId
    ) {
        postService.deletePost(memberId, postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 좋아요 토글
    @PostMapping("/{postId}/like")
    @Operation(summary = "좋아요 토글", description = "게시글 좋아요 상태를 토글합니다.")
    public ResponseEntity<ApiResponse<PostLikeResponse>> toggleLike(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId
    ) {
        PostLikeResponse response = postService.toggleLike(memberId, postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        CommentResponse response = postService.createComment(memberId, postId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 댓글 삭제
    @DeleteMapping("/{postId}/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글 작성자 또는 게시글 작성자가 댓글을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @CurrentMember Long memberId,
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId
    ) {
        postService.deleteComment(memberId, postId, commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    //게시글 생성
    @PostMapping
    @Operation(summary = "게시글 생성", description = "새 게시글을 생성합니다.")
    public ResponseEntity<ApiResponse<PostResponse>> create(
            @CurrentMember Long memberId,
            @RequestBody @Valid PostCreateRequest request
    ){
        PostResponse response = postService.create(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
