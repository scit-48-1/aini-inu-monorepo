package scit.ainiinu.walk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ThreadPatchRequest {

    @Size(max = 30)
    @Schema(description = "산책 모집글 제목입니다. null이면 변경하지 않습니다.", example = "아침 산책 메이트 구해요")
    private String title;

    @Size(max = 500)
    @Schema(description = "산책 모집글 설명입니다. null이면 변경하지 않습니다.", example = "한강공원에서 30분 산책 예정입니다.")
    private String description;

    @Schema(description = "산책 날짜입니다. null이면 변경하지 않습니다.", example = "2026-03-05")
    private LocalDate walkDate;

    @Schema(description = "산책 시작 시각(UTC)입니다. null이면 변경하지 않습니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime startTime;

    @Schema(description = "산책 종료 시각(UTC)입니다. null이면 변경하지 않습니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime endTime;

    @Schema(description = "채팅 타입입니다. null이면 변경하지 않습니다.", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "GROUP"})
    private String chatType;

    @Min(2)
    @Max(10)
    @Schema(description = "최대 참여 인원입니다. null이면 변경하지 않습니다.", example = "20")
    private Integer maxParticipants;

    @Schema(description = "비애견인 참여 허용 여부입니다. null이면 변경하지 않습니다.", example = "true")
    private Boolean allowNonPetOwner;

    @Schema(description = "상시 노출 여부입니다. null이면 변경하지 않습니다.", example = "true")
    private Boolean isVisibleAlways;

    @Valid
    @Schema(description = "장소 정보 객체입니다. null이면 변경하지 않습니다.", example = "예시 문자열")
    private ThreadCreateRequest.LocationRequest location;

    @Schema(description = "모집글에 연결할 반려견 ID 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[101,102]")
    private List<Long> petIds;

    @Valid
    @Schema(description = "모집 필터 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[\"예시 항목\"]")
    private List<ThreadCreateRequest.ThreadFilterRequest> filters;
}
