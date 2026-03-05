package scit.ainiinu.walk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class WalkDiaryCreateRequest {

    private Long threadId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank
    @Size(min = 1, max = 120)
    private String title;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 300)
    @NotBlank
    @Size(min = 1, max = 300)
    private String content;

    @Size(max = 5)
    private List<String> photoUrls;

    @NotNull
    private LocalDate walkDate;

    private Boolean isPublic;

    public boolean resolveIsPublic() {
        return isPublic == null || isPublic;
    }
}
