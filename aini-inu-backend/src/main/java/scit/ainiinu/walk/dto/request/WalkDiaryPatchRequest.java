package scit.ainiinu.walk.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class WalkDiaryPatchRequest {

    private Long threadId;

    @Size(max = 120)
    private String title;

    @Size(max = 300)
    private String content;

    @Size(max = 5)
    private List<String> photoUrls;

    private LocalDate walkDate;

    private Boolean isPublic;
}
