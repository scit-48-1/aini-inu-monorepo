package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostPetCreateRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String petName;

    private String breed;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String photoUrl;

    private String description;

    @NotNull
    private LocalDateTime lastSeenAt;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String lastSeenLocation;
}
