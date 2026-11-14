package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.service.NotificationService;
import com.workmates.posting.entity.CloseReason;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeadcountServiceTest {
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationStateMachine stateMachine = mock(ApplicationStateMachine.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final HeadcountService service = new HeadcountService(applications, stateMachine, notifications);

    private Posting posting;

    @BeforeEach
    void setUp() {
        posting = new Posting();
        posting.setId(42L);
        posting.setEmployerId(5L);
        posting.setTitle("Barista");
        posting.setStatus(PostingStatus.OPEN);
        posting.setHeadcount(2);
    }

    private static Application offer(long id, long studentId) {
        Application a = new Application();
        a.setId(id);
        a.setStudentId(studentId);
        a.setStatus(ApplicationStatus.OFFER);
        return a;
    }

    @Test
    void doesNothingWhileNotFull() {
        when(applications.countByPostingIdAndStatusIn(eq(42L), any())).thenReturn(1L);

        service.closeIfFilled(posting);

        assertEquals(PostingStatus.OPEN, posting.getStatus());
        verifyNoInteractions(stateMachine, notifications);
    }

    @Test
    void closesAndRejectsTheRestWhenFull() {
        Application a = offer(1, 11);
        Application b = offer(2, 12);
        when(applications.countByPostingIdAndStatusIn(eq(42L), any())).thenReturn(2L);
        when(applications.findByPostingIdAndStatusIn(eq(42L), any())).thenReturn(List.of(a, b));

        service.closeIfFilled(posting);

        assertEquals(PostingStatus.CLOSED, posting.getStatus());
        assertEquals(CloseReason.FILLED, posting.getCloseReason());
        verify(stateMachine).transition(a, ApplicationStatus.REJECTED, ActorType.SYSTEM, null,
                ApplicationStatusReason.POSITION_FILLED);
        verify(stateMachine).transition(b, ApplicationStatus.REJECTED, ActorType.SYSTEM, null,
                ApplicationStatusReason.POSITION_FILLED);
        verify(notifications, times(2)).notify(any(), eq(NotificationType.POSITION_FILLED), any(),
                eq(NotificationTargetType.APPLICATION), any());
        verify(notifications).notify(eq(5L), eq(NotificationType.POSTING_FILLED), any(),
                eq(NotificationTargetType.POSTING), eq(42L));
    }

    @Test
    void closedPostingIsLeftAlone() {
        posting.setStatus(PostingStatus.CLOSED);

        service.closeIfFilled(posting);

        verify(applications, never()).countByPostingIdAndStatusIn(any(), any());
        verifyNoInteractions(stateMachine, notifications);
    }
}
