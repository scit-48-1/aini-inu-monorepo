package scit.ainiinu.community.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.annotation.Public;
import scit.ainiinu.community.dto.PresignedImageRequest;
import scit.ainiinu.community.dto.PresignedImageResponse;
import scit.ainiinu.community.service.ImageUploadService;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "이미지 업로드 API")
@SecurityRequirement(name = "bearerAuth")
public class ImageController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/presigned-url")
    @Operation(summary = "Presigned URL 발급", description = "이미지 업로드용 1회성 Presigned URL을 발급합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "허용되지 않은 MIME 타입")
    })
    public ResponseEntity<ApiResponse<PresignedImageResponse>> createPresignedUrl(
            @CurrentMember Long memberId,
            @Valid @RequestBody PresignedImageRequest request
    ) {
        PresignedImageResponse response = imageUploadService.createPresignedUrl(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Public
    @PutMapping("/presigned-upload/{token}")
    @Operation(summary = "Presigned 업로드 수행", description = "발급받은 token으로 바이너리 이미지 업로드를 수행합니다.")
    @SecurityRequirements()
    public ResponseEntity<ApiResponse<Void>> uploadByPresignedUrl(
            @PathVariable String token,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @RequestBody byte[] payload
    ) {
        imageUploadService.uploadByToken(token, contentType, payload);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Public
    @GetMapping(value = "/local")
    @Operation(summary = "로컬 이미지 조회", description = "개발환경 로컬 스토리지 이미지를 조회합니다.")
    @SecurityRequirements()
    public ResponseEntity<Resource> getLocalFile(@RequestParam("key") String key) {
        Resource resource = imageUploadService.getLocalImage(key);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
