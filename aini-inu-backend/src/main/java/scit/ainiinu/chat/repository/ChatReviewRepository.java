package scit.ainiinu.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.chat.entity.ChatReview;

import java.util.List;
import java.util.Optional;

public interface ChatReviewRepository extends JpaRepository<ChatReview, Long> {

    boolean existsByChatRoomIdAndReviewerIdAndRevieweeId(Long chatRoomId, Long reviewerId, Long revieweeId);

    Optional<ChatReview> findTopByChatRoomIdAndReviewerIdOrderByCreatedAtDesc(Long chatRoomId, Long reviewerId);

    Slice<ChatReview> findByChatRoomIdOrderByCreatedAtDescIdDesc(Long chatRoomId, Pageable pageable);

    Slice<ChatReview> findByRevieweeIdOrderByCreatedAtDescIdDesc(Long revieweeId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.score), 0) FROM ChatReview r WHERE r.revieweeId = :revieweeId")
    double findAverageScoreByRevieweeId(@Param("revieweeId") Long revieweeId);

    long countByRevieweeId(Long revieweeId);

    @Query("SELECT r.score, COUNT(r) FROM ChatReview r WHERE r.revieweeId = :revieweeId GROUP BY r.score")
    List<Object[]> findScoreDistributionByRevieweeId(@Param("revieweeId") Long revieweeId);
}
