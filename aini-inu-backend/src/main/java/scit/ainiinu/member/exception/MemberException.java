package scit.ainiinu.member.exception;

import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.ErrorCode;

public class MemberException extends BusinessException {
    public MemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}
