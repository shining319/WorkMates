package com.workmates.notification.repository;

import com.workmates.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reading queries for the notification centre are added with US 5.2. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
