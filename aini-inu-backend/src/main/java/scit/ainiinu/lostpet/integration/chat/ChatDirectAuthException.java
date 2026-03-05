package scit.ainiinu.lostpet.integration.chat;

public class ChatDirectAuthException extends ChatDirectClientException {

    public ChatDirectAuthException(String message) {
        super(ChatDirectFailureType.AUTH, message);
    }

    public ChatDirectAuthException(String message, Throwable cause) {
        super(ChatDirectFailureType.AUTH, message, cause);
    }
}
