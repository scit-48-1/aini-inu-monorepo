package scit.ainiinu.lostpet.integration.chat;

public class ChatDirectConnectException extends ChatDirectClientException {

    public ChatDirectConnectException(String message) {
        super(ChatDirectFailureType.CONNECT, message);
    }

    public ChatDirectConnectException(String message, Throwable cause) {
        super(ChatDirectFailureType.CONNECT, message, cause);
    }
}
