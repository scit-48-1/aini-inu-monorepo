package scit.ainiinu.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.event.NotificationEvent;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.notification.dto.response.NotificationListResponse;
import scit.ainiinu.notification.entity.Notification;
import scit.ainiinu.notification.entity.NotificationType;
import scit.ainiinu.notification.repository.NotificationRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Notification createAndPublish(Long recipientMemberId, NotificationType type,
                                          String title, String message,
                                          Long referenceId, String referenceType) {
        Notification notification = Notification.create(
                recipientMemberId, type, title, message, referenceId, referenceType);
        Notification saved = notificationRepository.save(notification);

        applicationEventPublisher.publishEvent(NotificationEvent.of(
                recipientMemberId,
                type.name(),
                Map.of(
                        "notificationId", saved.getId(),
                        "title", title,
                        "message", message,
                        "referenceId", referenceId != null ? referenceId : "",
                        "referenceType", referenceType != null ? referenceType : ""
                )
        ));

        return saved;
    }

    public SliceResponse<NotificationListResponse> getNotifications(Long memberId, Pageable pageable) {
        Slice<NotificationListResponse> slice = notificationRepository
                .findByRecipientMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(NotificationListResponse::from);
        return SliceResponse.of(slice);
    }

    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByRecipientMemberIdAndIsReadFalse(memberId);
    }

    @Transactional
    public void markAsRead(Long memberId, Long notificationId) {
        notificationRepository.markAsRead(notificationId, memberId);
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
    }
}
