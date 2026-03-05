package scit.ainiinu.chat.repository;

import scit.ainiinu.chat.entity.Message;

import java.util.List;

public interface MessageRepositoryCustom {

    List<Message> findByRoomIdWithCursor(Long chatRoomId, Long cursor, int size, String direction);
}
