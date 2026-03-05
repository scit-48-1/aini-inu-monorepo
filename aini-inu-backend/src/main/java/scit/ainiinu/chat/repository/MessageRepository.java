package scit.ainiinu.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.chat.entity.Message;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long>, MessageRepositoryCustom {

    Optional<Message> findTopByChatRoomIdOrderByIdDesc(Long chatRoomId);
}
