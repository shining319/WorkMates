package com.workmates.notification.dto;

import com.workmates.notification.entity.Notification;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import java.time.Instant;

/** A single notification and the safe in-app page to open when it is clicked. */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        NotificationTargetType targetType,
        Long targetId,
        String targetUrl,
        boolean unread,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification n, String targetUrl) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(),
                n.getTargetType(), n.getTargetId(), targetUrl,
                n.getReadAt() == null, n.getCreatedAt(), n.getReadAt());
    }
}
