package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "산책일기에 연결된 스레드 요약 정보")
public class DiaryThreadSummary {

    @Schema(description = "스레드 ID", example = "100")
    private Long threadId;

    @Schema(description = "산책 날짜", example = "2026-03-05")
    private LocalDate walkDate;

    @Schema(description = "산책 시작 시각", example = "2026-03-05T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "산책 종료 시각", example = "2026-03-05T11:00:00")
    private LocalDateTime endTime;

    @Schema(description = "장소명", example = "서울숲")
    private String placeName;

    @Schema(description = "위도", example = "37.540000")
    private BigDecimal latitude;

    @Schema(description = "경도", example = "127.040000")
    private BigDecimal longitude;

    @Schema(description = "주소", example = "성동구 성수동")
    private String address;

    @Schema(description = "참여 반려견 목록")
    private List<PetCard> pets;

    @Getter
    @Builder
    @Schema(description = "반려견 카드 정보")
    public static class PetCard {
        @Schema(description = "반려견 ID", example = "1")
        private Long id;

        @Schema(description = "반려견 이름", example = "몽이")
        private String name;

        @Schema(description = "프로필 사진 URL", example = "https://cdn.example.com/pet.jpg")
        private String photoUrl;

        @Schema(description = "견종명", example = "골든 리트리버")
        private String breedName;
    }
}
