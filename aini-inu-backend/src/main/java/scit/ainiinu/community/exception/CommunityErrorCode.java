package scit.ainiinu.community.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "CO001", "게시물을 찾을 수 없습니다"),
    NOT_POST_OWNER(HttpStatus.FORBIDDEN, "CO002", "게시물 작성자가 아닙니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CO003", "댓글을 찾을 수 없습니다"),
    NOT_COMMENT_OWNER(HttpStatus.FORBIDDEN, "CO004", "댓글 삭제 권한이 없습니다"),
    INVALID_CONTENT_LENGTH(HttpStatus.BAD_REQUEST, "CO005", "내용이 너무 깁니다"),
    INVALID_UPLOAD_MIME(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "CO006", "허용되지 않는 이미지 타입입니다"),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "CO007", "파일 크기는 10MB를 초과할 수 없습니다"),
    INVALID_UPLOAD_PURPOSE(HttpStatus.BAD_REQUEST, "CO008", "업로드 목적 또는 파일 정보가 올바르지 않습니다"),
    UPLOAD_URL_EXPIRED_OR_INVALID(HttpStatus.FORBIDDEN, "CO009", "업로드 URL이 만료되었거나 유효하지 않습니다"),
    STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CO010", "저장소를 사용할 수 없습니다"),
    
    // 기존 유지 (명세 외 추가 제약)
    EXCEEDED_IMAGE_COUNT(HttpStatus.BAD_REQUEST, "CO901", "이미지는 최대 5개까지 업로드 가능합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
