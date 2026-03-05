package scit.ainiinu.lostpet.integration.chat;

public class ChatDirectResponseSchemaException extends ChatDirectClientException {

    public ChatDirectResponseSchemaException(String message) {
        super(ChatDirectFailureType.RESPONSE_SCHEMA, message);
    }

    public ChatDirectResponseSchemaException(String message, Throwable cause) {
        super(ChatDirectFailureType.RESPONSE_SCHEMA, message, cause);
    }
}
