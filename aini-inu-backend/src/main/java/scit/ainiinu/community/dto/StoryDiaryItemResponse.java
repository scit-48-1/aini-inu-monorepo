package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "스토리 뷰어에서 순차 노출되는 산책일기 항목")
public class StoryDiaryItemResponse {
    @Schema(description = "산책일기 ID", example = "101")
    private Long diaryId;

    @Schema(description = "산책일기 제목", example = "아침 산책")
    private String title;

    @Schema(description = "산책일기 본문", example = "강아지랑 공원 산책", maxLength = 300)
    private String content;

    @Schema(description = "산책일기 사진 URL 목록", example = "[\"https://cdn.example.com/diary-1.jpg\"]")
    private List<String> photoUrls;

    @Schema(description = "산책 날짜", example = "2026-03-04")
    private LocalDate walkDate;

    @Schema(description = "산책일기 작성 시각(UTC)", example = "2026-03-04T01:20:00Z")
    private OffsetDateTime createdAt;
}
