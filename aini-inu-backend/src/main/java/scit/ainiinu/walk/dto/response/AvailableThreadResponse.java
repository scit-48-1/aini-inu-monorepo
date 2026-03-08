package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.walk.entity.WalkThread;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AvailableThreadResponse {
    @Schema(description = "스레드 ID입니다.", example = "101")
    private Long threadId;
    @Schema(description = "스레드 제목입니다.", example = "한강공원 산책")
    private String title;
    @Schema(description = "산책 날짜입니다.", example = "2026-03-05")
    private LocalDate walkDate;
    @Schema(description = "장소명입니다.", example = "한강공원")
    private String placeName;
    @Schema(description = "시작 시간입니다.", example = "2026-03-05T14:00:00")
    private LocalDateTime startTime;

    public static AvailableThreadResponse from(WalkThread thread) {
        return AvailableThreadResponse.builder()
                .threadId(thread.getId())
                .title(thread.getTitle())
                .walkDate(thread.getWalkDate())
                .placeName(thread.getPlaceName())
                .startTime(thread.getStartTime())
                .build();
    }
}
