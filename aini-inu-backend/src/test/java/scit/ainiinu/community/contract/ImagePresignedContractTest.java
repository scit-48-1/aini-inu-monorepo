package scit.ainiinu.community.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.community.controller.ImageController;
import scit.ainiinu.community.dto.PresignedImageRequest;
import scit.ainiinu.community.dto.PresignedImageResponse;
import scit.ainiinu.community.exception.CommunityErrorCode;
import scit.ainiinu.community.service.ImageUploadService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageController.class)
class ImagePresignedContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImageUploadService imageUploadService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(
                argThat(parameter -> parameter.hasParameterAnnotation(CurrentMember.class))
        )).willReturn(true);
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("presigned URL 발급 요청이 유효하면 200 응답을 반환한다")
    void createPresignedUrlSuccess() throws Exception {
        PresignedImageRequest request = new PresignedImageRequest();
        request.setPurpose("POST");
        request.setFileName("post.jpg");
        request.setContentType("image/jpeg");

        PresignedImageResponse response = PresignedImageResponse.builder()
                .uploadUrl("http://localhost:8080/api/v1/images/presigned-upload/token-123")
                .imageUrl("http://localhost:8080/api/v1/images/local?key=community/post/file.jpg")
                .expiresIn(300L)
                .maxFileSizeBytes(10 * 1024 * 1024L)
                .build();

        given(imageUploadService.createPresignedUrl(anyLong(), any()))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/images/presigned-url")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl")
                        .value("http://localhost:8080/api/v1/images/presigned-upload/token-123"))
                .andExpect(jsonPath("$.data.imageUrl")
                        .value("http://localhost:8080/api/v1/images/local?key=community/post/file.jpg"))
                .andExpect(jsonPath("$.data.expiresIn").value(300))
                .andExpect(jsonPath("$.data.maxFileSizeBytes").value(10 * 1024 * 1024));
    }

    @Test
    @WithMockUser
    @DisplayName("presigned 업로드 토큰이 유효하지 않으면 403 에러를 반환한다")
    void presignedUploadInvalidToken() throws Exception {
        willThrow(new BusinessException(CommunityErrorCode.UPLOAD_URL_EXPIRED_OR_INVALID))
                .given(imageUploadService).uploadByToken(any(), any(), any());

        mockMvc.perform(put("/api/v1/images/presigned-upload/{token}", "invalid-token")
                        .with(csrf())
                        .contentType(MediaType.IMAGE_JPEG)
                        .content("binary-image".getBytes()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CO009"));
    }
}
