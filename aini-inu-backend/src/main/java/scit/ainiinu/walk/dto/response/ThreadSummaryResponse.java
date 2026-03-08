package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.walk.entity.WalkThread;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ThreadSummaryResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "제목입니다.", example = "아침 산책 메이트 구해요")
    private String title;
    @Schema(description = "설명 문구입니다.", example = "한강공원에서 30분 산책 예정입니다.")
    private String description;
    @Schema(description = "채팅 타입 코드입니다.", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "GROUP"})
    private String chatType;
    @Schema(description = "최대 참여 인원입니다.", example = "20")
    private Integer maxParticipants;
    @Schema(description = "현재 참여 인원입니다.", example = "20")
    private Integer currentParticipants;
    @Schema(description = "장소명입니다.", example = "몽이")
    private String placeName;
    @Schema(description = "위도입니다.", example = "37.566295")
    private BigDecimal latitude;
    @Schema(description = "경도입니다.", example = "126.977945")
    private BigDecimal longitude;
    @Schema(description = "시작 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime startTime;
    @Schema(description = "종료 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime endTime;
    @Schema(description = "상태 코드입니다.", example = "RECRUITING", allowableValues = {"RECRUITING", "EXPIRED", "DELETED"})
    private String status;

    @Schema(description = "첫 번째 참여 강아지의 프로필 이미지 URL입니다.", example = "https://example.com/pet.jpg")
    private String petImageUrl;

    @JsonProperty("isApplied")
    @Schema(description = "isApplied 값입니다.", example = "true")
    private boolean isApplied;

    public static ThreadSummaryResponse from(WalkThread walkThread) {
        return ThreadSummaryResponse.builder()
                .id(walkThread.getId())
                .title(walkThread.getTitle())
                .description(walkThread.getDescription())
                .chatType(walkThread.getChatType().name())
                .maxParticipants(walkThread.getMaxParticipants())
                .currentParticipants(0)
                .placeName(walkThread.getPlaceName())
                .latitude(walkThread.getLatitude())
                .longitude(walkThread.getLongitude())
                .startTime(walkThread.getStartTime())
                .endTime(walkThread.getEndTime())
                .status(walkThread.getStatus().name())
                .isApplied(false)
                .build();
    }
}
