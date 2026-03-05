package scit.ainiinu.community.dto;

import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.community.exception.CommunityErrorCode;

import java.util.Arrays;

public enum UploadPurpose {
    PROFILE,
    PET_PHOTO,
    POST,
    WALK_DIARY,
    LOST_PET,
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
