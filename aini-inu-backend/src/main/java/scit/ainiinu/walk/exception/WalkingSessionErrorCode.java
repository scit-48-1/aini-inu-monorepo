package scit.ainiinu.walk.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum WalkingSessionErrorCode implements ErrorCode {
    WALKING_SESSION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "WS409_ALREADY_ACTIVE", "이미 진행 중인 산책이 있습니다"),
    WALKING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "WS404_NOT_FOUND", "활성 산책 세션을 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
