package scit.ainiinu.walk.exception;

import scit.ainiinu.common.exception.BusinessException;

public class WalkDiaryException extends BusinessException {

    public WalkDiaryException(WalkDiaryErrorCode errorCode) {
        super(errorCode);
    }
}
