package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "작성자 회원 ID입니다.", example = "101")
    private Long authorId;
    @Schema(description = "제목입니다.", example = "아침 산책 메이트 구해요")
    private String title;
    @Schema(description = "설명 문구입니다.", example = "한강공원에서 30분 산책 예정입니다.")
    private String description;
    @Schema(description = "산책 날짜입니다.", example = "2026-03-05")
    private LocalDate walkDate;
    @Schema(description = "시작 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime startTime;
    @Schema(description = "종료 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime endTime;
    @Schema(description = "채팅 타입 코드입니다.", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "GROUP"})
    private String chatType;
    @Schema(description = "최대 참여 인원입니다.", example = "20")
    private Integer maxParticipants;
    @Schema(description = "현재 참여 인원입니다.", example = "20")
    private Integer currentParticipants;
    @Schema(description = "비애견인 참여 허용 여부입니다.", example = "true")
    private Boolean allowNonPetOwner;
    @Schema(description = "상시 노출 여부입니다.", example = "true")
    private Boolean isVisibleAlways;
    @Schema(description = "장소명입니다.", example = "몽이")
    private String placeName;
    @Schema(description = "위도입니다.", example = "37.566295")
    private BigDecimal latitude;
    @Schema(description = "경도입니다.", example = "126.977945")
    private BigDecimal longitude;
    @Schema(description = "상세 주소입니다.", example = "서울시 중구 세종대로 110")
    private String address;
    @Schema(description = "상태 코드입니다.", example = "RECRUITING", allowableValues = {"RECRUITING", "EXPIRED", "DELETED"})
    private String status;
    @Schema(description = "petIds 값입니다.", example = "[101,102]")
    private List<Long> petIds;
    @Schema(description = "applicants 값입니다.", example = "[\"예시 항목\"]")
    private List<ApplicantSummary> applicants;
    @Schema(description = "현재 사용자의 신청 여부입니다.", example = "false")
    private Boolean applied;
    @Schema(description = "참여 반려견 상세 정보입니다.")
    private List<PetSummary> pets;

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
                .applied(false)
                .pets(List.of())
                .build();
    }

    @Getter
    @Builder
    public static class ApplicantSummary {
        @Schema(description = "회원 ID입니다.", example = "101")
        private Long memberId;
        @Schema(description = "상태 코드입니다.", example = "RECRUITING", allowableValues = {"RECRUITING", "EXPIRED", "DELETED"})
        private String status;
        @Schema(description = "채팅방 ID입니다.", example = "101")
        private Long chatRoomId;
    }

    @Getter
    @Builder
    public static class PetSummary {
        @Schema(description = "반려견 ID입니다.", example = "1")
        private Long id;
        @Schema(description = "반려견 이름입니다.", example = "몽이")
        private String name;
        @Schema(description = "반려견 사진 URL입니다.")
        private String photoUrl;
        @Schema(description = "견종 이름입니다.", example = "시바견")
        private String breedName;
        @Schema(description = "나이입니다.", example = "3")
        private Integer age;
        @Schema(description = "성별입니다.", example = "MALE")
        private String gender;
        @Schema(description = "크기입니다.", example = "MEDIUM")
        private String size;
        @Schema(description = "MBTI입니다.", example = "ENFP")
        private String mbti;
        @Schema(description = "중성화 여부입니다.", example = "true")
        private Boolean isNeutered;
        @Schema(description = "산책 스타일 목록입니다.")
        private List<String> walkingStyles;
        @Schema(description = "성격 목록입니다.")
        private List<String> personalities;
    }
}
