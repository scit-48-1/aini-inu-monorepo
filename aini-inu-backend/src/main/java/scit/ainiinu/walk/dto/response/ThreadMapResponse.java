package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ThreadMapResponse {
    @Schema(description = "산책 모집글 ID입니다.", example = "101")
    private Long threadId;
    @Schema(description = "제목입니다.", example = "아침 산책 메이트 구해요")
    private String title;
    @Schema(description = "채팅 타입 코드입니다.", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "GROUP"})
    private String chatType;
    @Schema(description = "현재 참여 인원입니다.", example = "20")
    private Integer currentParticipants;
    @Schema(description = "최대 참여 인원입니다.", example = "20")
    private Integer maxParticipants;
    @Schema(description = "위도입니다.", example = "37.566295")
    private BigDecimal latitude;
    @Schema(description = "경도입니다.", example = "126.977945")
    private BigDecimal longitude;
    @Schema(description = "장소명입니다.", example = "몽이")
    private String placeName;
    @Schema(description = "첫 번째 참여 강아지의 프로필 이미지 URL입니다.", example = "https://example.com/pet.jpg")
    private String petImageUrl;
}
