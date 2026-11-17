package com.workmates.notification.service;

import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.security.Role;
import com.workmates.notification.dto.MarkAllReadResponse;
import com.workmates.notification.dto.NotificationResponse;
import com.workmates.notification.dto.UnreadCountResponse;
import com.workmates.notification.entity.Notification;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.repository.NotificationRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US 5.2: authenticated, user-scoped notification centre. */
@Service
public class NotificationCentreService {
    public static final int PAGE_SIZE = 20;

    private final NotificationRepository notifications;
    private final ApplicationRepository applications;

    public NotificationCentreService(NotificationRepository notifications, ApplicationRepository applications) {
        this.notifications = notifications;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, Role role, int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Page must be zero or greater");
        }
        return notifications.findByRecipientIdOrderByCreatedAtDescIdDesc(userId,
                PageRequest.of(page, PAGE_SIZE)).map(notification -> toResponse(notification, role));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notifications.countByRecipientIdAndReadAtIsNull(userId));
    }

    /** Idempotent: an already-read notification remains read. Non-owners get NOT_FOUND. */
    @Transactional
    public NotificationResponse markRead(Long userId, Role role, Long notificationId) {
        Notification notification = notifications.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        notification.markRead(Instant.now());
        return toResponse(notification, role);
    }

    @Transactional
    public MarkAllReadResponse markAllRead(Long userId) {
        int updated = notifications.markAllRead(userId, Instant.now());
        return new MarkAllReadResponse(updated, 0);
    }

    private NotificationResponse toResponse(Notification notification, Role role) {
        return NotificationResponse.from(notification, targetUrl(notification, role));
    }

    /**
     * APPLICATION target IDs refer to applications, not postings. Link to implemented
     * posting/job detail pages until the application's dedicated detail UI is built.
     * Never trust a URL from the notification message itself.
     */
    private String targetUrl(Notification notification, Role role) {
        boolean employer = role == Role.EMPLOYER;
        if (notification.getTargetType() == NotificationTargetType.POSTING) {
            return (employer ? "/postings/" : "/jobs/") + notification.getTargetId();
        }
        return applications.findPostingIdById(notification.getTargetId())
                .map(id -> (employer ? "/postings/" : "/jobs/") + id)
                .orElse(employer ? "/postings" : "/jobs");
    }
}
