package com.workmates.notification.controller;

import com.workmates.common.PageInfo;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.notification.dto.MarkAllReadResponse;
import com.workmates.notification.dto.NotificationResponse;
import com.workmates.notification.dto.UnreadCountResponse;
import com.workmates.notification.service.NotificationCentreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Both STUDENT and EMPLOYER can access their own notifications only. */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "US 5.2 notification centre")
public class NotificationController {
    private final NotificationCentreService centre;

    public NotificationController(NotificationCentreService centre) {
        this.centre = centre;
    }

    @GetMapping
    @Operation(summary = "My notifications, newest first, 20 per page")
    public Result<List<NotificationResponse>> list(CurrentUser user,
            @RequestParam(defaultValue = "0") int page) {
        Page<NotificationResponse> found = centre.list(user.getId(), page);
        return Result.ok(found.getContent(), new PageInfo(found.getNumber(), found.getSize(),
                found.getTotalElements(), found.getTotalPages()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread count for header bell (client polls every 30 seconds)")
    public Result<UnreadCountResponse> unreadCount(CurrentUser user) {
        return Result.ok(centre.unreadCount(user.getId()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark my notification read and return its target identifiers")
    public Result<NotificationResponse> markRead(CurrentUser user, @PathVariable Long notificationId) {
        return Result.ok(centre.markRead(user.getId(), notificationId));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all my notifications read")
    public Result<MarkAllReadResponse> markAllRead(CurrentUser user) {
        return Result.ok(centre.markAllRead(user.getId()));
    }
}
