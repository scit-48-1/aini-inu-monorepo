package scit.ainiinu.walk.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.walk.entity.WalkDiary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class WalkDiaryResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "회원 ID입니다.", example = "101")
    private Long memberId;
    @Schema(description = "산책 모집글 ID입니다.", example = "101")
    private Long threadId;
    @Schema(description = "제목입니다.", example = "아침 산책 메이트 구해요")
    private String title;
    @Schema(description = "산책일기 본문입니다.", example = "한강공원에서 30분 산책했어요.", maxLength = 300)
    private String content;
    @Schema(description = "사진 URL 목록입니다.", example = "[\"https://cdn.example.com/sample.jpg\"]")
    private List<String> photoUrls;
    @Schema(description = "산책 날짜입니다.", example = "2026-03-05")
    private LocalDate walkDate;

    @JsonProperty("isPublic")
    @Schema(description = "공개 여부입니다.", example = "true")
    private boolean isPublic;
    @Schema(description = "연결된 산책 모집글 상태 코드입니다.", example = "RECRUITING", allowableValues = {"RECRUITING", "EXPIRED", "DELETED"})
    private String linkedThreadStatus;
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime createdAt;
    @Schema(description = "수정 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime updatedAt;

    public static WalkDiaryResponse from(WalkDiary walkDiary, String linkedThreadStatus) {
        return WalkDiaryResponse.builder()
                .id(walkDiary.getId())
                .memberId(walkDiary.getMemberId())
                .threadId(walkDiary.getThreadId())
                .title(walkDiary.getTitle())
                .content(walkDiary.getContent())
                .photoUrls(new ArrayList<>(walkDiary.getPhotoUrls()))
                .walkDate(walkDiary.getWalkDate())
                .isPublic(Boolean.TRUE.equals(walkDiary.getIsPublic()))
                .linkedThreadStatus(linkedThreadStatus)
                .createdAt(walkDiary.getCreatedAt())
                .updatedAt(walkDiary.getUpdatedAt())
                .build();
    }
}
