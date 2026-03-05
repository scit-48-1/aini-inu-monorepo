package scit.ainiinu.walk.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ThreadErrorCode implements ErrorCode {
    NON_PET_OWNER_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "T403_NON_PET_OWNER_CREATE_FORBIDDEN", "비애견인은 스레드를 생성할 수 없습니다"),
    THREAD_OWNER_ONLY(HttpStatus.FORBIDDEN, "T403_THREAD_OWNER_ONLY", "작성자만 처리할 수 있습니다"),
    APPLY_FORBIDDEN_SELF(HttpStatus.FORBIDDEN, "T403_APPLY_FORBIDDEN_SELF", "본인 스레드에는 신청할 수 없습니다"),
    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "T404_THREAD_NOT_FOUND", "스레드를 찾을 수 없습니다"),
    THREAD_APPLY_NOT_FOUND(HttpStatus.NOT_FOUND, "T404_THREAD_APPLY_NOT_FOUND", "신청 정보를 찾을 수 없습니다"),
    CAPACITY_FULL(HttpStatus.CONFLICT, "T409_CAPACITY_FULL", "스레드 정원이 가득 찼습니다"),
    THREAD_ALREADY_ACTIVE(HttpStatus.CONFLICT, "T409_THREAD_ALREADY_ACTIVE", "이미 활성 스레드가 존재합니다"),
    THREAD_EXPIRED(HttpStatus.CONFLICT, "T409_THREAD_EXPIRED", "만료된 스레드입니다"),
    INVALID_CHAT_TYPE(HttpStatus.BAD_REQUEST, "T400_INVALID_CHAT_TYPE", "지원하지 않는 채팅 타입입니다"),
    INVALID_THREAD_REQUEST(HttpStatus.BAD_REQUEST, "T400_INVALID_THREAD_REQUEST", "잘못된 스레드 요청입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
