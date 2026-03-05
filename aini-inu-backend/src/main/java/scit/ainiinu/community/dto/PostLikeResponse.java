package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostLikeResponse {
    @Schema(description = "isLiked 값입니다.", example = "true")
    private boolean isLiked;
    @Schema(description = "likeCount 값입니다.", example = "20")
    private int likeCount;
}
