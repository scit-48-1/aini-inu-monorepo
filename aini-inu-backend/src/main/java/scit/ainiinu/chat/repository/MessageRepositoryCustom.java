package scit.ainiinu.chat.repository;

import scit.ainiinu.chat.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageRepositoryCustom {

    List<Message> findByRoomIdWithCursor(Long chatRoomId, Long cursor, int size, String direction);

    /**
     * Batch-fetch the latest message per chat room (N+1 prevention).
     * Returns a map of chatRoomId → latest Message.
     */
    Map<Long, Message> findLastMessagesByRoomIds(List<Long> chatRoomIds);
}
