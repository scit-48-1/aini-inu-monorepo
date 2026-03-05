package scit.ainiinu.walk.dto.response;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.walk.entity.WalkThread;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ThreadResponse {
    private Long id;
    private Long authorId;
    private String title;
    private String description;
    private LocalDate walkDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String chatType;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Boolean allowNonPetOwner;
    private Boolean isVisibleAlways;
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String status;
    private List<Long> petIds;
    private List<ApplicantSummary> applicants;

    public static ThreadResponse from(WalkThread walkThread) {
        return ThreadResponse.builder()
                .id(walkThread.getId())
                .authorId(walkThread.getAuthorId())
                .title(walkThread.getTitle())
                .description(walkThread.getDescription())
                .walkDate(walkThread.getWalkDate())
                .startTime(walkThread.getStartTime())
                .endTime(walkThread.getEndTime())
                .chatType(walkThread.getChatType().name())
                .maxParticipants(walkThread.getMaxParticipants())
                .currentParticipants(0)
                .allowNonPetOwner(walkThread.getAllowNonPetOwner())
                .isVisibleAlways(walkThread.getIsVisibleAlways())
                .placeName(walkThread.getPlaceName())
                .latitude(walkThread.getLatitude())
                .longitude(walkThread.getLongitude())
                .address(walkThread.getAddress())
                .status(walkThread.getStatus().name())
                .petIds(List.of())
                .applicants(List.of())
                .build();
    }

    @Getter
    @Builder
    public static class ApplicantSummary {
        private Long memberId;
        private String status;
        private Long chatRoomId;
    }
}
