package scit.ainiinu.walk.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ThreadPatchRequest {

    @Size(max = 30)
    private String title;

    @Size(max = 500)
    private String description;

    private LocalDate walkDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String chatType;

    @Min(2)
    @Max(10)
    private Integer maxParticipants;

    private Boolean allowNonPetOwner;

    private Boolean isVisibleAlways;

    @Valid
    private ThreadCreateRequest.LocationRequest location;

    private List<Long> petIds;

    @Valid
    private List<ThreadCreateRequest.ThreadFilterRequest> filters;
}
