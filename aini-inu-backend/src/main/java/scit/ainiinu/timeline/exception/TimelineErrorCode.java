package scit.ainiinu.timeline.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum TimelineErrorCode implements ErrorCode {
    TIMELINE_NOT_PUBLIC(HttpStatus.FORBIDDEN, "TL403_TIMELINE_NOT_PUBLIC", "비공개 타임라인입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TL404_MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
