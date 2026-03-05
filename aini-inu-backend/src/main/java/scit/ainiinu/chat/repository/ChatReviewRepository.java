package scit.ainiinu.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import scit.ainiinu.chat.entity.ChatReview;

import java.util.Optional;

public interface ChatReviewRepository extends JpaRepository<ChatReview, Long> {

    boolean existsByChatRoomIdAndReviewerIdAndRevieweeId(Long chatRoomId, Long reviewerId, Long revieweeId);

    Optional<ChatReview> findTopByChatRoomIdAndReviewerIdOrderByCreatedAtDesc(Long chatRoomId, Long reviewerId);

    Slice<ChatReview> findByChatRoomIdOrderByCreatedAtDescIdDesc(Long chatRoomId, Pageable pageable);
}
