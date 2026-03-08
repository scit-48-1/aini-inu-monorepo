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

    @NotNull
    @Schema(description = "산책 모집글 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long threadId;

    @Schema(
            description = "산책일기 제목입니다.",
            example = "한강공원 저녁 산책 기록",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    @NotBlank
    @Size(min = 1, max = 120)
    private String title;

    @Schema(
            description = "산책일기 본문입니다.",
            example = "오늘은 한강공원에서 30분 산책했고 다른 강아지들과 인사도 했어요.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 300
    )
    @NotBlank
    @Size(min = 1, max = 300)
    private String content;

    @Size(max = 5)
    @Schema(description = "사진 URL 목록입니다.", example = "[\"https://cdn.example.com/sample.jpg\"]")
    private List<String> photoUrls;

    @NotNull
    @Schema(description = "산책 날짜입니다.", example = "2026-03-05", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate walkDate;

    @Schema(description = "공개 여부입니다.", example = "true")
    private Boolean isPublic;

    public boolean resolveIsPublic() {
        return isPublic == null || isPublic;
    }
}
