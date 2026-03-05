package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PresignedImageRequest {

    @Schema(
            description = "이미지 업로드 목적 코드입니다.",
            example = "POST",
            allowableValues = {"PROFILE", "PET_PHOTO", "POST", "WALK_DIARY", "LOST_PET", "SIGHTING"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String purpose;

    @Schema(
            description = "업로드할 원본 파일명입니다.",
            example = "walk-photo.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String fileName;

    @Schema(
            description = "업로드 파일 MIME 타입입니다.",
            example = "image/jpeg",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String contentType;
}
