package scit.ainiinu.lostpet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LostPetMatchApproveRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    private Long sightingId;
}
