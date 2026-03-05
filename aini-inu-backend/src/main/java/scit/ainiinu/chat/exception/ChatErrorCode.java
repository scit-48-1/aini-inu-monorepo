package scit.ainiinu.chat.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CH400_INVALID_REQUEST", "잘못된 채팅 요청입니다"),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "CH400_INVALID_CURSOR", "유효하지 않은 커서입니다"),
    INVALID_MESSAGE_CONTENT(HttpStatus.BAD_REQUEST, "CH400_INVALID_MESSAGE_CONTENT", "메시지는 1자 이상 500자 이하로 입력해야 합니다"),
    INVALID_REVIEW_SCORE(HttpStatus.BAD_REQUEST, "CH400_INVALID_REVIEW_SCORE", "리뷰 점수는 1~5 범위여야 합니다"),
    INVALID_WALK_CONFIRM_ACTION(HttpStatus.BAD_REQUEST, "CH400_INVALID_WALK_CONFIRM_ACTION", "지원하지 않는 산책확인 액션입니다"),

    ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH403_ROOM_ACCESS_DENIED", "채팅방 접근 권한이 없습니다"),

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH404_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다"),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH404_MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다"),

    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "CH409_REVIEW_ALREADY_EXISTS", "이미 리뷰를 작성했습니다"),
    ROOM_ALREADY_LEFT(HttpStatus.CONFLICT, "CH409_ROOM_ALREADY_LEFT", "이미 채팅방에서 나간 상태입니다"),
    ROOM_CLOSED(HttpStatus.CONFLICT, "CH409_ROOM_CLOSED", "종료된 채팅방에는 메시지를 보낼 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
