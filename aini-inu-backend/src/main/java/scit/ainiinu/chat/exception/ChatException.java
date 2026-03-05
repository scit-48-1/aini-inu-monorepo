package scit.ainiinu.chat.exception;

import scit.ainiinu.common.exception.BusinessException;

public class ChatException extends BusinessException {
    public ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }
}
