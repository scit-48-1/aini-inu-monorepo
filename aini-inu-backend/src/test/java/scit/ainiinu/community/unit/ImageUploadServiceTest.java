package scit.ainiinu.community.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.community.config.CommunityStorageProperties;
import scit.ainiinu.community.dto.PresignedImageRequest;
import scit.ainiinu.community.dto.PresignedImageResponse;
import scit.ainiinu.community.exception.CommunityErrorCode;
import scit.ainiinu.community.service.ImageUploadService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadServiceTest {

    private ImageUploadService imageUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        CommunityStorageProperties properties = new CommunityStorageProperties();
        properties.setPublicBaseUrl("http://localhost:8080");
        properties.getLocal().setBaseDir(tempDir.toString());
        properties.getPresigned().setExpiresSeconds(300L);
        imageUploadService = new ImageUploadService(properties);
    }

    @Nested
    @DisplayName("Presigned URL 발급")
    class IssuePresignedUrl {

        @Test
        @DisplayName("유효한 요청이면 uploadUrl/imageUrl을 반환한다")
        void issueSuccess() {
            PresignedImageRequest request = new PresignedImageRequest();
            request.setPurpose("POST");
            request.setFileName("sample.jpg");
            request.setContentType("image/jpeg");

            PresignedImageResponse response = imageUploadService.createPresignedUrl(1L, request);

            assertThat(response.getUploadUrl()).contains("/api/v1/images/presigned-upload/");
            assertThat(response.getImageUrl()).contains("/api/v1/images/local?key=");
            assertThat(response.getExpiresIn()).isEqualTo(300L);
            assertThat(response.getMaxFileSizeBytes()).isEqualTo(10 * 1024 * 1024L);
        }

        @Test
        @DisplayName("허용되지 않은 MIME 타입이면 예외가 발생한다")
        void failUnsupportedMimeType() {
            PresignedImageRequest request = new PresignedImageRequest();
            request.setPurpose("POST");
            request.setFileName("sample.gif");
            request.setContentType("image/gif");

            assertThatThrownBy(() -> imageUploadService.createPresignedUrl(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.INVALID_UPLOAD_MIME);
        }
    }

    @Nested
    @DisplayName("Presigned 업로드")
    class UploadByToken {

        @Test
        @DisplayName("유효한 토큰/콘텐츠로 업로드하면 로컬 파일이 저장된다")
        void uploadSuccess() throws Exception {
            PresignedImageRequest request = new PresignedImageRequest();
            request.setPurpose("POST");
            request.setFileName("sample.jpg");
            request.setContentType("image/jpeg");

            PresignedImageResponse issued = imageUploadService.createPresignedUrl(1L, request);
            String uploadToken = issued.getUploadUrl().substring(issued.getUploadUrl().lastIndexOf('/') + 1);

            imageUploadService.uploadByToken(uploadToken, "image/jpeg", "jpeg-data".getBytes());

            String encodedKey = issued.getImageUrl().substring(issued.getImageUrl().indexOf("key=") + 4);
            String decodedKey = URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
            Path savedPath = tempDir.resolve(decodedKey);

            assertThat(Files.exists(savedPath)).isTrue();
            assertThat(Files.readAllBytes(savedPath)).isEqualTo("jpeg-data".getBytes());
        }

        @Test
        @DisplayName("동일 토큰 재사용 시 예외가 발생한다")
        void failTokenReuse() {
            PresignedImageRequest request = new PresignedImageRequest();
            request.setPurpose("POST");
            request.setFileName("sample.jpg");
            request.setContentType("image/jpeg");

            PresignedImageResponse issued = imageUploadService.createPresignedUrl(1L, request);
            String uploadToken = issued.getUploadUrl().substring(issued.getUploadUrl().lastIndexOf('/') + 1);

            imageUploadService.uploadByToken(uploadToken, "image/jpeg", "jpeg-data".getBytes());

            assertThatThrownBy(() -> imageUploadService.uploadByToken(uploadToken, "image/jpeg", "jpeg-data".getBytes()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.UPLOAD_URL_EXPIRED_OR_INVALID);
        }

        @Test
        @DisplayName("MIME 타입이 다르면 예외가 발생한다")
        void failMimeMismatch() {
            PresignedImageRequest request = new PresignedImageRequest();
            request.setPurpose("POST");
            request.setFileName("sample.jpg");
            request.setContentType("image/jpeg");

            PresignedImageResponse issued = imageUploadService.createPresignedUrl(1L, request);
            String uploadToken = issued.getUploadUrl().substring(issued.getUploadUrl().lastIndexOf('/') + 1);

            assertThatThrownBy(() -> imageUploadService.uploadByToken(uploadToken, "image/png", "jpeg-data".getBytes()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.INVALID_UPLOAD_MIME);
        }
    }
}
