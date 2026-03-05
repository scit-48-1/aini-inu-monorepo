package scit.ainiinu.chat.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatReviewCreateRequest {

    @NotNull
    private Long revieweeId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    private String comment;
}
