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
    private Long id;
    private Long memberId;
    private Long threadId;
    private String title;
    @Schema(maxLength = 300)
    private String content;
    private List<String> photoUrls;
    private LocalDate walkDate;

    @JsonProperty("isPublic")
    private boolean isPublic;
    private String linkedThreadStatus;
    private LocalDateTime createdAt;
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
