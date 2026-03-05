package scit.ainiinu.walk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "연결된 스레드 ID입니다. null이면 변경하지 않습니다.", example = "101")
    private Long threadId;

    @Size(max = 120)
    @Schema(description = "일기 제목입니다. null이면 변경하지 않습니다.", example = "아침 산책 메이트 구해요")
    private String title;

    @Size(max = 300)
    @Schema(description = "일기 본문입니다. null이면 변경하지 않습니다.", example = "강아지와 즐거운 산책을 했어요.")
    private String content;

    @Size(max = 5)
    @Schema(description = "일기 사진 URL 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[\"https://cdn.example.com/sample.jpg\"]")
    private List<String> photoUrls;

    @Schema(description = "산책 날짜입니다. null이면 변경하지 않습니다.", example = "2026-03-05")
    private LocalDate walkDate;

    @Schema(description = "공개 여부입니다. null이면 변경하지 않습니다.", example = "true")
    private Boolean isPublic;
}
