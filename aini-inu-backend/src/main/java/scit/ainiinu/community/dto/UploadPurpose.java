package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.community.exception.CommunityErrorCode;

import java.util.Arrays;

@Schema(description = "이미지 업로드 목적 코드")
public enum UploadPurpose {
    @Schema(description = "회원 프로필 이미지")
    PROFILE,
    @Schema(description = "반려견 프로필 이미지")
    PET_PHOTO,
    @Schema(description = "커뮤니티 게시글 이미지")
    POST,
    @Schema(description = "산책일기 이미지")
    WALK_DIARY,
    @Schema(description = "실종 신고 이미지")
    LOST_PET,
    @Schema(description = "목격 제보 이미지")
    SIGHTING;

    public static UploadPurpose from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(CommunityErrorCode.INVALID_UPLOAD_PURPOSE);
        }

        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.INVALID_UPLOAD_PURPOSE));
    }
}
