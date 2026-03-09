package scit.ainiinu.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findByRecipientMemberIdOrderByCreatedAtDesc(Long recipientMemberId, Pageable pageable);

    long countByRecipientMemberIdAndIsReadFalse(Long recipientMemberId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipientMemberId = :recipientMemberId AND n.isRead = false")
    int markAsRead(@Param("id") Long id, @Param("recipientMemberId") Long recipientMemberId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientMemberId = :recipientMemberId AND n.isRead = false")
    int markAllAsRead(@Param("recipientMemberId") Long recipientMemberId);
}
