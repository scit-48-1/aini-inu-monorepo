package scit.ainiinu.lostpet.integration.chat;

import lombok.Getter;

@Getter
public class ChatDirectClientException extends RuntimeException {

    private final ChatDirectFailureType failureType;

    public ChatDirectClientException(ChatDirectFailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }

    public ChatDirectClientException(ChatDirectFailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }
}
