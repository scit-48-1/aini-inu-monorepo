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
    THREAD_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "WD400_THREAD_NOT_COMPLETED", "완료되지 않은 스레드입니다"),
    NOT_THREAD_PARTICIPANT(HttpStatus.FORBIDDEN, "WD403_NOT_THREAD_PARTICIPANT", "스레드 참여자만 일기를 작성할 수 있습니다"),
    DIARY_ALREADY_EXISTS(HttpStatus.CONFLICT, "WD409_DIARY_ALREADY_EXISTS", "이미 해당 스레드에 일기를 작성했습니다"),
    IMAGE_COUNT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "WD422_IMAGE_COUNT_EXCEEDED", "이미지는 최대 5장까지 가능합니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
