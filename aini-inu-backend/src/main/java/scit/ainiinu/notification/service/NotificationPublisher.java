package scit.ainiinu.notification.service;

import scit.ainiinu.notification.dto.response.NotificationResponse;

public interface NotificationPublisher {

    void sendToUser(Long memberId, NotificationResponse response);
}
