package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record LostPetMatchCandidateResponse(
        @Schema(description = "분석 세션 ID입니다.", example = "101")
        Long sessionId,
        @Schema(description = "sightingId 값입니다.", example = "101")
        Long sightingId,
        @Schema(description = "finderId 값입니다.", example = "101")
        Long finderId,
        @Schema(description = "제보 사진 URL입니다.", example = "https://cdn/photo.jpg")
        String photoUrl,
        @Schema(description = "발견 위치입니다.", example = "서울시 강남구 역삼동")
        String foundLocation,
        @Schema(description = "발견 시간입니다.")
        LocalDateTime foundAt,
        @Schema(description = "제보 메모입니다.", nullable = true)
        String memo,
        @Schema(description = "제보자 닉네임입니다.", example = "멍멍이사랑")
        String finderNickname,
        @Schema(description = "scoreSimilarity 값입니다.", example = "4.5")
        BigDecimal scoreSimilarity,
        @Schema(description = "scoreDistance 값입니다.", example = "4.5")
        BigDecimal scoreDistance,
        @Schema(description = "scoreRecency 값입니다.", example = "4.5")
        BigDecimal scoreRecency,
        @Schema(description = "scoreTotal 값입니다.", example = "4.5")
        BigDecimal scoreTotal,
        @Schema(description = "rank 값입니다.", example = "20")
        Integer rank,
        @Schema(description = "상태 코드입니다.", example = "CANDIDATE", allowableValues = {"CANDIDATE", "APPROVED"})
        String status
) {
}
