package scit.ainiinu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WalkConfirmRequest {

    @NotBlank
    @Schema(description = "동작 액션 코드입니다.", example = "CONFIRM", allowableValues = {"CONFIRM", "CANCEL"})
    private String action;
}
