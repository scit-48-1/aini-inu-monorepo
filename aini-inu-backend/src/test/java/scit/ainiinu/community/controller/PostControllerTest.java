package scit.ainiinu.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.community.dto.CommentCreateRequest;
import scit.ainiinu.community.dto.CommentResponse;
import scit.ainiinu.community.dto.PostCreateRequest;
import scit.ainiinu.community.dto.PostDetailResponse;
import scit.ainiinu.community.dto.PostLikeResponse;
import scit.ainiinu.community.dto.PostResponse;
import scit.ainiinu.community.dto.PostUpdateRequest;
import scit.ainiinu.community.exception.CommunityErrorCode;
import scit.ainiinu.community.service.PostService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(
                argThat(parameter -> parameter.hasParameterAnnotation(CurrentMember.class))
        )).willReturn(true);
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Nested
    @DisplayName("게시글 생성")
    class CreatePost {
        @Test
        @WithMockUser
        @DisplayName("유효한 요청으로 게시글을 생성하면 성공한다")
        void create_post_success() throws Exception {
            // given
            PostResponse dummy = new PostResponse();
            given(postService.create(anyLong(), any())).willReturn(dummy);

            String body = """
                    {
                      "content": "오늘 산책 최고!",
                      "imageUrls": ["a.jpg", "b.jpg"]
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/posts")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("게시글 목록 조회")
    class GetPosts {
        @Test
        @WithMockUser
        @DisplayName("게시글 목록을 페이징하여 조회하면 SliceResponse를 반환한다")
        void get_posts_success() throws Exception {
            // given
            PostResponse postResponse = new PostResponse();
            postResponse.setId(1L);
            postResponse.setContent("테스트 게시글");

            SliceResponse<PostResponse> sliceResponse = new SliceResponse<>(
                    List.of(postResponse),
                    0,
                    20,
                    true,
                    true,
                    false
            );
            given(postService.getPosts(anyLong(), any())).willReturn(sliceResponse);

            // when & then
            mockMvc.perform(get("/api/v1/posts")
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].content").value("테스트 게시글"))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }
    }

    @Nested
    @DisplayName("게시글 상세 조회")
    class GetPostDetail {
        @Test
        @WithMockUser
        @DisplayName("게시글 ID로 조회하면 상세 정보와 댓글 목록을 반환한다")
        void get_post_detail_success() throws Exception {
            // given
            Long postId = 1L;
            PostDetailResponse dummyResponse = new PostDetailResponse();
            dummyResponse.setId(postId);
            dummyResponse.setContent("상세 내용");

            CommentResponse commentDummy = new CommentResponse();
            commentDummy.setId(10L);
            commentDummy.setContent("댓글 내용");
            dummyResponse.setComments(List.of(commentDummy));

            given(postService.getPostDetail(anyLong(), anyLong())).willReturn(dummyResponse);

            // when & then
            mockMvc.perform(get("/api/v1/posts/{postId}", postId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(postId))
                    .andExpect(jsonPath("$.data.comments[0].content").value("댓글 내용"));
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {
        @Test
        @WithMockUser
        @DisplayName("작성자가 게시글을 수정하면 성공한다")
        void update_post_success() throws Exception {
            // given
            Long postId = 1L;
            PostResponse updatedResponse = new PostResponse();
            updatedResponse.setId(postId);
            updatedResponse.setContent("수정된 내용");

            given(postService.updatePost(anyLong(), eq(postId), any(PostUpdateRequest.class)))
                    .willReturn(updatedResponse);

            String body = """
                    {
                      "content": "수정된 내용",
                      "imageUrls": ["new.jpg"]
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").value("수정된 내용"));
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 게시글 수정 시 CO001 에러를 반환한다")
        void update_post_not_found() throws Exception {
            // given
            Long postId = 999L;
            given(postService.updatePost(anyLong(), eq(postId), any(PostUpdateRequest.class)))
                    .willThrow(new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

            String body = """
                    {
                      "content": "수정된 내용"
                    }
                    """;

            // when & then
            mockMvc.perform(patch("/api/v1/posts/{postId}", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("CO001"));
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {
        @Test
        @WithMockUser
        @DisplayName("작성자가 게시글을 삭제하면 성공한다")
        void delete_post_success() throws Exception {
            // given
            Long postId = 1L;
            willDoNothing().given(postService).deletePost(anyLong(), eq(postId));

            // when & then
            mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 게시글 삭제 시 CO001 에러를 반환한다")
        void delete_post_not_found() throws Exception {
            // given
            Long postId = 999L;
            willThrow(new BusinessException(CommunityErrorCode.POST_NOT_FOUND))
                    .given(postService).deletePost(anyLong(), eq(postId));

            // when & then
            mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("CO001"));
        }
    }

    @Nested
    @DisplayName("좋아요 토글")
    class ToggleLike {
        @Test
        @WithMockUser
        @DisplayName("좋아요 토글 요청 시 isLiked와 likeCount를 반환한다")
        void toggle_like_success() throws Exception {
            // given
            Long postId = 1L;
            PostLikeResponse likeResponse = new PostLikeResponse(true, 5);
            given(postService.toggleLike(anyLong(), eq(postId))).willReturn(likeResponse);

            // when & then
            mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.liked").value(true))
                    .andExpect(jsonPath("$.data.likeCount").value(5));
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 게시글 좋아요 시 CO001 에러를 반환한다")
        void toggle_like_post_not_found() throws Exception {
            // given
            Long postId = 999L;
            given(postService.toggleLike(anyLong(), eq(postId)))
                    .willThrow(new BusinessException(CommunityErrorCode.POST_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/v1/posts/{postId}/like", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("CO001"));
        }
    }

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {
        @Test
        @WithMockUser
        @DisplayName("유효한 요청으로 댓글을 작성하면 성공한다")
        void create_comment_success() throws Exception {
            // given
            Long postId = 1L;
            CommentResponse commentResponse = new CommentResponse();
            commentResponse.setId(10L);
            commentResponse.setContent("새 댓글");

            given(postService.createComment(anyLong(), eq(postId), any(CommentCreateRequest.class)))
                    .willReturn(commentResponse);

            String body = """
                    {
                      "content": "새 댓글"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.content").value("새 댓글"));
        }

        @Test
        @WithMockUser
        @DisplayName("내용이 비어있으면 400 에러를 반환한다")
        void create_comment_empty_content() throws Exception {
            // given
            Long postId = 1L;
            String body = """
                    {
                      "content": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @WithMockUser
        @DisplayName("댓글 목록을 조회하면 SliceResponse를 반환한다")
        void get_comments_success() throws Exception {
            // given
            Long postId = 1L;
            CommentResponse comment = new CommentResponse();
            comment.setId(10L);
            comment.setContent("댓글");

            SliceResponse<CommentResponse> sliceResponse = new SliceResponse<>(
                    List.of(comment),
                    0,
                    20,
                    true,
                    true,
                    false
            );
            given(postService.getComments(anyLong(), eq(postId), any())).willReturn(sliceResponse);

            // when & then
            mockMvc.perform(get("/api/v1/posts/{postId}/comments", postId)
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(10))
                    .andExpect(jsonPath("$.data.content[0].content").value("댓글"));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {
        @Test
        @WithMockUser
        @DisplayName("작성자가 댓글을 삭제하면 성공한다")
        void delete_comment_success() throws Exception {
            // given
            Long postId = 1L;
            Long commentId = 10L;
            willDoNothing().given(postService).deleteComment(anyLong(), eq(postId), eq(commentId));

            // when & then
            mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 댓글 삭제 시 CO003 에러를 반환한다")
        void delete_comment_not_found() throws Exception {
            // given
            Long postId = 1L;
            Long commentId = 999L;
            willThrow(new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND))
                    .given(postService).deleteComment(anyLong(), eq(postId), eq(commentId));

            // when & then
            mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("CO003"));
        }
    }
}
