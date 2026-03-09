package scit.ainiinu.lostpet.integration.chat;

public interface ChatRoomDirectClient {
    Long createDirectRoom(Long memberId, Long partnerId, String origin, String roomTitle);
}
