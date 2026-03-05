package scit.ainiinu.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.chat.entity.ChatParticipantPet;

import java.util.Collection;
import java.util.List;

public interface ChatParticipantPetRepository extends JpaRepository<ChatParticipantPet, Long> {

    List<ChatParticipantPet> findAllByChatParticipantIdIn(Collection<Long> chatParticipantIds);
}
