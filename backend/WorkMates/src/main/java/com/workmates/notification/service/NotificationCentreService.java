package com.workmates.notification.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.notification.dto.MarkAllReadResponse;
import com.workmates.notification.dto.NotificationResponse;
import com.workmates.notification.dto.UnreadCountResponse;
import com.workmates.notification.entity.Notification;
import com.workmates.notification.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US 5.2: authenticated, recipient-scoped notification centre. */
@Service
public class NotificationCentreService {
    public static final int PAGE_SIZE = 20;

    private final NotificationRepository notifications;

    public NotificationCentreService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Page must be zero or greater");
        }
        // Ordering and recipient filtering happen in the repository, before pagination.
        return notifications.findByRecipientIdOrderByCreatedAtDescIdDesc(userId,
                PageRequest.of(page, PAGE_SIZE)).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notifications.countByRecipientIdAndReadAtIsNull(userId));
    }

    /** Idempotent: an already-read notification keeps its first readAt value. */
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notifications.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        notification.markRead(Instant.now());
        return NotificationResponse.from(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllRead(Long userId) {
        int updated = notifications.markAllRead(userId, Instant.now());
        return new MarkAllReadResponse(updated, 0);
    }
}
