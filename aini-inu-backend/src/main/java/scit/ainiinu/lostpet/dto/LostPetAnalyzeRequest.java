package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostPetAnalyzeRequest {
    private static final int COORDINATE_SCALE = 6;

    @NotNull(message = "lostPetId는 필수입니다.")
    @Schema(description = "실종 신고 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long lostPetId;

    @Schema(description = "image 값입니다.", example = "예시 문자열")
    private String image;

    @Schema(description = "imageUrl 값입니다.", example = "https://cdn.example.com/sample.jpg")
    private String imageUrl;

    @Schema(description = "mode 값입니다.", example = "예시 문자열")
    private String mode;

    @Schema(description = "queryText 값입니다.", example = "예시 문자열")
    private String queryText;

    @Schema(description = "위도입니다.", example = "37.566295")
    private Double latitude;
    @Schema(description = "경도입니다.", example = "126.977945")
    private Double longitude;

    @AssertTrue(message = "image 또는 imageUrl 중 하나는 필수입니다.")
    public boolean isImageProvided() {
        return hasText(image) || hasText(imageUrl);
    }

    public String resolveImageSource() {
        if (hasText(image)) {
            return image;
        }
        return imageUrl;
    }

    public String resolveMode() {
        if (hasText(mode)) {
            return mode.trim();
        }
        return "LOST";
    }

    public LostPetAnalyzeRequest normalizeForAi() {
        String resolvedImage = resolveImageSource();
        return LostPetAnalyzeRequest.builder()
                .lostPetId(lostPetId)
                .image(resolvedImage)
                .imageUrl(resolvedImage)
                .mode(resolveMode())
                .queryText(queryText)
                .latitude(normalizeCoordinate(latitude))
                .longitude(normalizeCoordinate(longitude))
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Double normalizeCoordinate(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
