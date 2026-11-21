package com.workmates.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.notification.entity.Notification;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

/** Mockito coverage for the US 5.2 notification centre business service. */
class NotificationCentreServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationCentreService service = new NotificationCentreService(repository);

    private Notification notification(long id, long recipient, Instant createdAt) {
        Notification n = new Notification(recipient, NotificationType.APPLICATION_RECEIVED,
                "New application", NotificationTargetType.APPLICATION, 88L);
        ReflectionTestUtils.setField(n, "id", id);
        ReflectionTestUtils.setField(n, "createdAt", createdAt);
        return n;
    }

    @Test
    void listIsRecipientScopedNewestFirstAndExactlyTwentyPerPage() {
        long me = 7L;
        PageRequest firstPage = PageRequest.of(0, 20);
        List<Notification> newestFirst = new ArrayList<>();
        Instant base = Instant.parse("2026-11-16T12:00:00Z");
        for (int i = 20; i >= 1; i--) {
            newestFirst.add(notification(i, me, base.plusSeconds(i)));
        }
        when(repository.findByRecipientIdOrderByCreatedAtDescIdDesc(me, firstPage))
                .thenReturn(new PageImpl<>(newestFirst, firstPage, 21));

        var result = service.list(me, 0);

        assertEquals(20, result.getContent().size());
        assertEquals(21, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(20L, result.getContent().getFirst().id());
        assertEquals(1L, result.getContent().getLast().id());
        assertEquals(NotificationTargetType.APPLICATION, result.getContent().getFirst().targetType());
        assertEquals(88L, result.getContent().getFirst().targetId());
        assertTrue(result.getContent().getFirst().unread());
        verify(repository).findByRecipientIdOrderByCreatedAtDescIdDesc(me, firstPage);
        // The repository method orders by createdAt DESC, id DESC before paging.
        // Only the authenticated recipient ID is supplied to that repository method.
    }

    @Test
    void listSecondPageRequestsSameRecipientAndTwentyPerPage() {
        PageRequest secondPage = PageRequest.of(1, 20);
        var older = notification(1, 7, Instant.parse("2026-11-16T11:00:00Z"));
        when(repository.findByRecipientIdOrderByCreatedAtDescIdDesc(7L, secondPage))
                .thenReturn(new PageImpl<>(List.of(older), secondPage, 21));

        var result = service.list(7L, 1);

        assertEquals(List.of(1L), result.getContent().stream().map(n -> n.id()).toList());
        verify(repository).findByRecipientIdOrderByCreatedAtDescIdDesc(7L, secondPage);
    }

    @Test
    void rejectsNegativePageWithoutReadingAnything() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.list(7L, -1));
        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verifyNoInteractions(repository);
    }

    @Test
    void markReadOnlyMyNotificationAndReturnUpdatedData() {
        Notification own = notification(13L, 7L, Instant.parse("2026-11-16T12:00:00Z"));
        when(repository.findByIdAndRecipientId(13L, 7L)).thenReturn(Optional.of(own));

        var result = service.markRead(7L, 13L);

        assertEquals(13L, result.id());
        assertFalse(result.unread());
        assertNotNull(result.readAt());
        assertEquals(result.readAt(), own.getReadAt());
        verify(repository).findByIdAndRecipientId(13L, 7L);
    }

    @Test
    void markReadOnAnotherUsersNotificationReturnsNotFound() {
        // An ID that belongs to somebody else is not found for the current recipient.
        when(repository.findByIdAndRecipientId(13L, 7L)).thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.markRead(7L, 13L));

        assertEquals(ErrorCode.NOT_FOUND, error.getCode());
        verify(repository).findByIdAndRecipientId(13L, 7L);
        verify(repository, never()).findById(13L);
    }

    @Test
    void markingAlreadyReadNotificationAgainPreservesOriginalReadAt() {
        Notification own = notification(13L, 7L, Instant.parse("2026-11-16T12:00:00Z"));
        Instant firstRead = Instant.parse("2026-11-16T13:00:00Z");
        own.markRead(firstRead);
        when(repository.findByIdAndRecipientId(13L, 7L)).thenReturn(Optional.of(own));

        var again = service.markRead(7L, 13L);

        assertFalse(again.unread());
        assertEquals(firstRead, again.readAt());
        assertEquals(firstRead, own.getReadAt());
    }

    @Test
    void markAllReadUpdatesOnlyTheCurrentRecipient() {
        when(repository.markAllRead(eq(7L), any(Instant.class))).thenReturn(3);

        var result = service.markAllRead(7L);

        assertEquals(3, result.updated());
        assertEquals(0, result.unreadCount());
        verify(repository).markAllRead(eq(7L), any(Instant.class));
    }

    @Test
    void markAllReadWorksWithZeroUnreadNotifications() {
        when(repository.markAllRead(eq(7L), any(Instant.class))).thenReturn(0);

        var result = service.markAllRead(7L);

        assertEquals(0, result.updated());
        assertEquals(0, result.unreadCount());
    }

    @Test
    void unreadCountReadsOnlyTheCurrentRecipientsUnreadRows() {
        when(repository.countByRecipientIdAndReadAtIsNull(7L)).thenReturn(106L);

        var response = service.unreadCount(7L);

        assertEquals(106L, response.unreadCount());
        verify(repository).countByRecipientIdAndReadAtIsNull(7L);
    }
}
