package scit.ainiinu.community.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedImageResponse {
    private String uploadUrl;
    private String imageUrl;
    private Long expiresIn;
    private Long maxFileSizeBytes;
}
