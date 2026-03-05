package scit.ainiinu.walk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ThreadCreateRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank
    @Size(min = 1, max = 30)
    private String title;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank
    @Size(min = 1, max = 500)
    private String description;

    @NotNull
    private LocalDate walkDate;

    @NotNull
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String chatType;

    @NotNull
    @Min(2)
    @Max(10)
    private Integer maxParticipants;

    private Boolean allowNonPetOwner;

    private Boolean isVisibleAlways;

    @NotNull
    @Valid
    private LocationRequest location;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<Long> petIds;

    @Valid
    private List<ThreadFilterRequest> filters;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LocationRequest {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String placeName;

        @NotNull
        private Double latitude;

        @NotNull
        private Double longitude;

        private String address;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ThreadFilterRequest {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String type;

        private List<String> values;

        private Boolean isRequired;
    }
}
