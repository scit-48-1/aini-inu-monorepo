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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String purpose;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String fileName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String contentType;
}
