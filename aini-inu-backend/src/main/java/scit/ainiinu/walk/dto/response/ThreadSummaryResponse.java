package scit.ainiinu.walk.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.walk.entity.WalkThread;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ThreadSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private String chatType;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    @JsonProperty("isApplied")
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
