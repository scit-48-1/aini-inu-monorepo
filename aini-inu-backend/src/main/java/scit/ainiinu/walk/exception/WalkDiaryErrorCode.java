package scit.ainiinu.walk.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum WalkDiaryErrorCode implements ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "WD400_INVALID_REQUEST", "잘못된 일기 요청입니다"),
    DIARY_OWNER_ONLY(HttpStatus.FORBIDDEN, "WD403_DIARY_OWNER_ONLY", "작성자만 처리할 수 있습니다"),
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "WD404_DIARY_NOT_FOUND", "일기를 찾을 수 없습니다"),
    DIARY_PRIVATE(HttpStatus.NOT_FOUND, "WD404_DIARY_PRIVATE", "비공개 일기입니다"),
    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "WD404_THREAD_NOT_FOUND", "연결된 스레드를 찾을 수 없습니다"),
    IMAGE_COUNT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "WD422_IMAGE_COUNT_EXCEEDED", "이미지는 최대 5장까지 가능합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
