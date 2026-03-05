package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(min = 1, max = 500, message = "댓글은 500자를 초과할 수 없습니다.")
    private String content;
}
