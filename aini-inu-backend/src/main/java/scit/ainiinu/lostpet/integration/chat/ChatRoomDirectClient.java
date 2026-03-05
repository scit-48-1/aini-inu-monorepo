package scit.ainiinu.lostpet.integration.chat;

public interface ChatRoomDirectClient {
    Long createDirectRoom(Long partnerId, String authorizationHeader);
}
