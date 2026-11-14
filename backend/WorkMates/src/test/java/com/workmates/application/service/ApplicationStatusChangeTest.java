package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.Role;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.service.NotificationService;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.service.PostingService;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.service.StudentProfileService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US 4.3 (employer changes) and US 4.4 (student answers an offer). The state machine is a mock. */
class ApplicationStatusChangeTest {
    private static final Long STUDENT_ID = 3L;
    private static final Long EMPLOYER_ID = 5L;
    private static final Long POSTING_ID = 42L;
    private static final Long APPLICATION_ID = 99L;
    private static final CurrentUser EMPLOYER = new CurrentUser(EMPLOYER_ID, Role.EMPLOYER);
    private static final CurrentUser STUDENT = new CurrentUser(STUDENT_ID, Role.STUDENT);

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationStateMachine stateMachine = mock(ApplicationStateMachine.class);
    private final PostingService postings = mock(PostingService.class);
    private final StudentProfileService profiles = mock(StudentProfileService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final HeadcountService headcount = mock(HeadcountService.class);
    private final ApplicationService service =
            new ApplicationService(applications, stateMachine, postings, profiles, notifications, headcount);

    private Posting posting;
    private Application application;

    @BeforeEach
    void setUp() {
        posting = new Posting();
        posting.setId(POSTING_ID);
        posting.setEmployerId(EMPLOYER_ID);
        posting.setTitle("Barista");
        posting.setStatus(PostingStatus.OPEN);
        posting.setHeadcount(2);
        application = new Application();
        application.setId(APPLICATION_ID);
        application.setPostingId(POSTING_ID);
        application.setStudentId(STUDENT_ID);
        application.setStatus(ApplicationStatus.APPLIED);
        when(applications.findPostingIdById(APPLICATION_ID)).thenReturn(Optional.of(POSTING_ID));
        when(applications.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(postings.lockPosting(POSTING_ID)).thenReturn(posting);
        StudentProfileResponse profile = new StudentProfileResponse();
        profile.setName("Ann Murphy");
        when(profiles.get(STUDENT_ID)).thenReturn(profile);
    }

    private void stateMachineRefuses() {
        when(stateMachine.transition(any(), any(), any(), any(), any())).thenThrow(new BusinessException(
                ErrorCode.INVALID_STATUS_TRANSITION, "This status change is not allowed. Current status: X"));
    }

    // ---- US 4.3

    @Test
    void employerAdvancesAndStudentIsNotified() {
        service.changeStatus(EMPLOYER, APPLICATION_ID, ApplicationStatus.REVIEWED);

        verify(stateMachine).transition(application, ApplicationStatus.REVIEWED, ActorType.EMPLOYER, EMPLOYER_ID, null);
        verify(notifications).notify(eq(STUDENT_ID), eq(NotificationType.STATUS_CHANGED),
                eq("Barista: your application moved to Reviewed"), eq(NotificationTargetType.APPLICATION),
                eq(APPLICATION_ID));
        verify(headcount, never()).closeIfFilled(any());
    }

    @Test
    void employerCanReject() {
        service.changeStatus(EMPLOYER, APPLICATION_ID, ApplicationStatus.REJECTED);

        verify(stateMachine).transition(application, ApplicationStatus.REJECTED, ActorType.EMPLOYER, EMPLOYER_ID, null);
    }

    @Test
    void refusedChangeWritesNoNotification() {
        // skipping a step, rejecting an OFFER or changing a final status: the state machine says no
        stateMachineRefuses();

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(EMPLOYER, APPLICATION_ID, ApplicationStatus.OFFER));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, e.getCode());
        verifyNoInteractions(notifications);
    }

    @Test
    void manuallyClosedPostingStillAllowsChanges() {
        posting.setStatus(PostingStatus.CLOSED);

        service.changeStatus(EMPLOYER, APPLICATION_ID, ApplicationStatus.REVIEWED);

        verify(stateMachine).transition(application, ApplicationStatus.REVIEWED, ActorType.EMPLOYER, EMPLOYER_ID, null);
    }

    @Test
    void employerWhoDoesNotOwnThePostingGetsNotFound() {
        CurrentUser other = new CurrentUser(77L, Role.EMPLOYER);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(other, APPLICATION_ID, ApplicationStatus.REVIEWED));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
        verifyNoInteractions(stateMachine, notifications);
    }

    @Test
    void unknownApplicationGetsNotFound() {
        when(applications.findPostingIdById(1L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(EMPLOYER, 1L, ApplicationStatus.REVIEWED));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
    }

    @Test
    void completedIsNotOpenYet() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(EMPLOYER, APPLICATION_ID, ApplicationStatus.COMPLETED));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, e.getCode());
        verifyNoInteractions(stateMachine, notifications);
    }

    // ---- US 4.4

    @Test
    void studentAcceptsAndEmployerIsNotified() {
        application.setStatus(ApplicationStatus.OFFER);
        when(applications.countByPostingIdAndStatusIn(eq(POSTING_ID), any())).thenReturn(1L);

        service.changeStatus(STUDENT, APPLICATION_ID, ApplicationStatus.ACCEPTED);

        verify(stateMachine).transition(application, ApplicationStatus.ACCEPTED, ActorType.STUDENT, STUDENT_ID, null);
        verify(notifications).notify(eq(EMPLOYER_ID), eq(NotificationType.OFFER_ACCEPTED),
                eq("Ann Murphy accepted the offer for Barista"), eq(NotificationTargetType.APPLICATION),
                eq(APPLICATION_ID));
        verify(headcount).closeIfFilled(posting);
    }

    @Test
    void studentDeclinesAndEmployerIsNotified() {
        application.setStatus(ApplicationStatus.OFFER);

        service.changeStatus(STUDENT, APPLICATION_ID, ApplicationStatus.DECLINED);

        verify(stateMachine).transition(application, ApplicationStatus.DECLINED, ActorType.STUDENT, STUDENT_ID, null);
        verify(notifications).notify(eq(EMPLOYER_ID), eq(NotificationType.OFFER_DECLINED),
                eq("Ann Murphy declined the offer for Barista"), eq(NotificationTargetType.APPLICATION),
                eq(APPLICATION_ID));
        verify(headcount, never()).closeIfFilled(any());
    }

    @Test
    void answerOutsideOfferIsInvalidTransition() {
        stateMachineRefuses();

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(STUDENT, APPLICATION_ID, ApplicationStatus.ACCEPTED));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, e.getCode());
        verifyNoInteractions(notifications);
    }

    @Test
    void bothAnswersAreRefusedWhenAlreadyRejectedAsFilled() {
        application.setStatus(ApplicationStatus.REJECTED);
        application.setStatusReason(ApplicationStatusReason.POSITION_FILLED);

        for (ApplicationStatus answer : new ApplicationStatus[] {ApplicationStatus.ACCEPTED, ApplicationStatus.DECLINED}) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> service.changeStatus(STUDENT, APPLICATION_ID, answer));
            assertEquals(ErrorCode.POSITION_FILLED, e.getCode());
        }
        verifyNoInteractions(stateMachine, notifications);
    }

    @Test
    void acceptIsRefusedWhenHeadcountAlreadyReached() {
        application.setStatus(ApplicationStatus.OFFER);
        when(applications.countByPostingIdAndStatusIn(eq(POSTING_ID), any())).thenReturn(2L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(STUDENT, APPLICATION_ID, ApplicationStatus.ACCEPTED));

        assertEquals(ErrorCode.POSITION_FILLED, e.getCode());
        verifyNoInteractions(stateMachine, notifications);
        verify(headcount, never()).closeIfFilled(any());
    }

    @Test
    void studentWhoDoesNotOwnTheApplicationGetsNotFound() {
        application.setStatus(ApplicationStatus.OFFER);
        CurrentUser other = new CurrentUser(88L, Role.STUDENT);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeStatus(other, APPLICATION_ID, ApplicationStatus.ACCEPTED));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
        verifyNoInteractions(stateMachine, notifications);
    }
}
