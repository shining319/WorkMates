package com.workmates.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.workmates.notification.entity.Notification;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.repository.NotificationRepository;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class NotificationServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(repository);

    @Test
    void savesAnUnreadNotificationWithTheGivenFields() {
        service.notify(7L, NotificationType.APPLICATION_RECEIVED, "Ann applied for Barista",
                NotificationTargetType.APPLICATION, 99L);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        Notification notification = saved.getValue();
        assertEquals(7L, notification.getRecipientId());
        assertEquals(NotificationType.APPLICATION_RECEIVED, notification.getType());
        assertEquals("Ann applied for Barista", notification.getMessage());
        assertEquals(NotificationTargetType.APPLICATION, notification.getTargetType());
        assertEquals(99L, notification.getTargetId());
        assertNull(notification.getReadAt());
    }

    @Test
    void acceptsAMessageOfExactlyTheMaximumLength() {
        service.notify(7L, NotificationType.POSTING_FILLED, "x".repeat(NotificationService.MAX_MESSAGE_LENGTH),
                NotificationTargetType.POSTING, 1L);

        verify(repository).save(any(Notification.class));
    }

    @Test
    void rejectsATooLongMessageAndSavesNothing() {
        assertThrows(IllegalArgumentException.class, () -> service.notify(7L, NotificationType.POSTING_FILLED,
                "x".repeat(NotificationService.MAX_MESSAGE_LENGTH + 1), NotificationTargetType.POSTING, 1L));

        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void rejectsABlankOrMissingMessage() {
        assertThrows(IllegalArgumentException.class, () -> service.notify(7L, NotificationType.POSTING_FILLED,
                " ", NotificationTargetType.POSTING, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.notify(7L, NotificationType.POSTING_FILLED,
                null, NotificationTargetType.POSTING, 1L));

        verify(repository, never()).save(any(Notification.class));
    }

    /** The notification must roll back together with the change it reports, so a caller transaction is required. */
    @Test
    void mustRunInsideTheCallersTransaction() throws NoSuchMethodException {
        Method notify = NotificationService.class.getMethod("notify", Long.class, NotificationType.class,
                String.class, NotificationTargetType.class, Long.class);

        assertEquals(Propagation.MANDATORY, notify.getAnnotation(Transactional.class).propagation());
    }
}
