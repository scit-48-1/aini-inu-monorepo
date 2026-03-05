package scit.ainiinu.walk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ThreadCreateRequest {

    @Schema(
            description = "산책 모집글 제목입니다.",
            example = "주말 오전 산책 메이트 구해요",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    @NotBlank
    @Size(min = 1, max = 30)
    private String title;

    @Schema(
            description = "산책 모집글 설명입니다.",
            example = "서울숲에서 40분 정도 가볍게 산책할 분을 찾고 있어요.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    @NotBlank
    @Size(min = 1, max = 500)
    private String description;

    @NotNull
    @Schema(description = "산책 날짜입니다.", example = "2026-03-05", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate walkDate;

    @NotNull
    @Schema(description = "시작 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "종료 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime endTime;

    @Schema(
            description = "채팅 타입 코드입니다.",
            example = "INDIVIDUAL",
            allowableValues = {"INDIVIDUAL", "GROUP"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    private String chatType;

    @NotNull
    @Min(2)
    @Max(10)
    @Schema(description = "최대 참여 인원입니다.", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer maxParticipants;

    @Schema(description = "비애견인 참여 허용 여부입니다.", example = "true")
    private Boolean allowNonPetOwner;

    @Schema(description = "상시 노출 여부입니다.", example = "true")
    private Boolean isVisibleAlways;

    @NotNull
    @Valid
    @Schema(description = "산책 장소 정보 객체입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocationRequest location;

    @Schema(description = "모집글에 연결할 반려견 ID 목록입니다.", example = "[101,102]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private List<Long> petIds;

    @Valid
    @Schema(description = "모집 필터 목록입니다.", example = "[{\"type\":\"AGE_GROUP\",\"values\":[\"20\",\"30\"],\"isRequired\":false}]")
    private List<ThreadFilterRequest> filters;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LocationRequest {
        @Schema(description = "장소명입니다.", example = "서울숲", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String placeName;

        @NotNull
        @Schema(description = "위도입니다.", example = "37.566295", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double latitude;

        @NotNull
        @Schema(description = "경도입니다.", example = "126.977945", requiredMode = Schema.RequiredMode.REQUIRED)
        private Double longitude;

        @Schema(description = "상세 주소입니다.", example = "서울시 중구 세종대로 110")
        private String address;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ThreadFilterRequest {
        @Schema(description = "필터 유형 코드입니다.", example = "AGE_GROUP", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String type;

        @Schema(description = "필터 값 목록입니다.", example = "[\"20\",\"30\"]")
        private List<String> values;

        @Schema(description = "필터 필수 여부입니다.", example = "false")
        private Boolean isRequired;
    }
}
