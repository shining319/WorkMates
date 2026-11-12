package com.workmates.notification.service;

import com.workmates.notification.entity.Notification;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Creates in-app notifications. */
@Service
public class NotificationService {
    /** Same as the width of {@code notifications.message}. */
    static final int MAX_MESSAGE_LENGTH = 300;

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    /**
     * Saves a notification in the caller's transaction, so it is rolled back together with the change it reports.
     * The caller picks the recipient: never the person who made the change. Build the message with
     * {@link NotificationMessages}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void notify(Long recipientId, NotificationType type, String message,
                       NotificationTargetType targetType, Long targetId) {
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("A notification message needs 1 to " + MAX_MESSAGE_LENGTH + " characters");
        }
        notifications.save(new Notification(recipientId, type, message, targetType, targetId));
    }
}
