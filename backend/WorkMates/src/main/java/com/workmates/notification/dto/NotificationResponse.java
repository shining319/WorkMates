package com.workmates.notification.dto;

import com.workmates.notification.entity.Notification;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import java.time.Instant;

/** Notification data only. The frontend resolves the destination from targetType/targetId. */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        NotificationTargetType targetType,
        Long targetId,
        boolean unread,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(),
                n.getTargetType(), n.getTargetId(),
                n.getReadAt() == null, n.getCreatedAt(), n.getReadAt());
    }
}
