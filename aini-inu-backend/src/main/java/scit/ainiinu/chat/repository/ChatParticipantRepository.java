package scit.ainiinu.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.chat.entity.ChatParticipant;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findAllByChatRoomId(Long chatRoomId);

    List<ChatParticipant> findAllByChatRoomIdAndLeftAtIsNull(Long chatRoomId);

    Optional<ChatParticipant> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    Optional<ChatParticipant> findByChatRoomIdAndMemberIdAndLeftAtIsNull(Long chatRoomId, Long memberId);

    long countByChatRoomIdAndLeftAtIsNull(Long chatRoomId);

    boolean existsByChatRoomIdAndMemberIdAndLeftAtIsNull(Long chatRoomId, Long memberId);
}
