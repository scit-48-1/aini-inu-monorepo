package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedImageResponse {
    @Schema(description = "uploadUrl 값입니다.", example = "https://cdn.example.com/sample.jpg")
    private String uploadUrl;
    @Schema(description = "imageUrl 값입니다.", example = "https://cdn.example.com/sample.jpg")
    private String imageUrl;
    @Schema(description = "expiresIn 값입니다.", example = "101")
    private Long expiresIn;
    @Schema(description = "maxFileSizeBytes 값입니다.", example = "20")
    private Long maxFileSizeBytes;
}
