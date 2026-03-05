package scit.ainiinu.member.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import scit.ainiinu.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "M002", "닉네임이 유효하지 않습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "M003", "이미 사용 중인 닉네임입니다."),
    BANNED_MEMBER(HttpStatus.FORBIDDEN, "M005", "정지된 회원입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M007", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M008", "이메일 또는 비밀번호가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
