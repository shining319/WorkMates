package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.workmates.application.dto.StatusChange;
import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusHistory;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationStatusHistoryRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class ApplicationStateMachineTest {
    private static final Long APPLICATION_ID = 7L;
    private static final Long USER_ID = 11L;

    private final ApplicationStatusHistoryRepository history = mock(ApplicationStatusHistoryRepository.class);
    private final ApplicationStateMachine machine = new ApplicationStateMachine(history);

    /** The allowed changes, written out once more from architecture section 8.2: "FROM>TO:actors". */
    private static final String[] EXPECTED = {
            "APPLIED>REVIEWED:EMPLOYER",
            "REVIEWED>INTERVIEW:EMPLOYER",
            "INTERVIEW>OFFER:EMPLOYER",
            "APPLIED>REJECTED:EMPLOYER,SYSTEM",
            "REVIEWED>REJECTED:EMPLOYER,SYSTEM",
            "INTERVIEW>REJECTED:EMPLOYER,SYSTEM",
            "OFFER>REJECTED:SYSTEM",
            "OFFER>ACCEPTED:STUDENT",
            "OFFER>DECLINED:STUDENT",
            "ACCEPTED>COMPLETED:STUDENT,EMPLOYER"};

    private static Set<ActorType> expectedActors(ApplicationStatus from, ApplicationStatus to) {
        for (String entry : EXPECTED) {
            String[] parts = entry.split(":");
            if (parts[0].equals(from + ">" + to)) {
                return Arrays.stream(parts[1].split(",")).map(ActorType::valueOf).collect(Collectors.toSet());
            }
        }
        return Set.of();
    }

    private static Application application(ApplicationStatus status) {
        Application application = new Application();
        application.setId(APPLICATION_ID);
        application.setPostingId(42L);
        application.setStudentId(USER_ID);
        application.setStatus(status);
        return application;
    }

    @Test
    void everyFromToActorCombinationFollowsTheTable() {
        int allowed = 0;
        for (ApplicationStatus from : ApplicationStatus.values()) {
            for (ApplicationStatus to : ApplicationStatus.values()) {
                for (ActorType actor : ActorType.values()) {
                    Application application = application(from);
                    String combination = from + " -> " + to + " by " + actor;

                    if (expectedActors(from, to).contains(actor)) {
                        StatusChange change = machine.transition(application, to, actor, USER_ID, null);
                        assertEquals(to, application.getStatus(), combination);
                        assertEquals(new StatusChange(APPLICATION_ID, from, to, actor), change, combination);
                        allowed++;
                    } else {
                        BusinessException e = assertThrows(BusinessException.class,
                                () -> machine.transition(application, to, actor, USER_ID, null), combination);
                        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, e.getCode(), combination);
                        assertEquals(from, application.getStatus(), combination);
                    }
                }
            }
        }
        // 3 employer steps + 3 rejects x 2 actors + system-only offer reject + 2 student replies + 2 completers
        assertEquals(14, allowed);
    }

    @Test
    void employerCanNotRejectAnOfferOnlyTheSystemCan() {
        assertThrows(BusinessException.class,
                () -> machine.transition(application(ApplicationStatus.OFFER), ApplicationStatus.REJECTED,
                        ActorType.EMPLOYER, USER_ID, null));

        Application application = application(ApplicationStatus.OFFER);
        machine.transition(application, ApplicationStatus.REJECTED, ActorType.SYSTEM, null,
                ApplicationStatusReason.POSITION_FILLED);

        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
    }

    @Test
    void employerMovesOneStepAtATime() {
        assertThrows(BusinessException.class,
                () -> machine.transition(application(ApplicationStatus.APPLIED), ApplicationStatus.OFFER,
                        ActorType.EMPLOYER, USER_ID, null));
        assertThrows(BusinessException.class,
                () -> machine.transition(application(ApplicationStatus.APPLIED), ApplicationStatus.INTERVIEW,
                        ActorType.EMPLOYER, USER_ID, null));
    }

    @Test
    void finalStatusesHaveNoWayOut() {
        for (ApplicationStatus finalStatus : new ApplicationStatus[] {
                ApplicationStatus.REJECTED, ApplicationStatus.DECLINED, ApplicationStatus.COMPLETED}) {
            for (ApplicationStatus to : ApplicationStatus.values()) {
                for (ActorType actor : ActorType.values()) {
                    assertThrows(BusinessException.class,
                            () -> machine.transition(application(finalStatus), to, actor, USER_ID, null),
                            finalStatus + " -> " + to + " by " + actor);
                }
            }
        }
        verify(history, never()).save(any(ApplicationStatusHistory.class));
    }

    @Test
    void refusalMessageNamesTheCurrentStatus() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> machine.transition(application(ApplicationStatus.INTERVIEW), ApplicationStatus.REVIEWED,
                        ActorType.EMPLOYER, USER_ID, null));

        assertEquals("This status change is not allowed. Current status: INTERVIEW", e.getMessage());
    }

    @Test
    void refusedChangeWritesNoHistoryAndKeepsTheApplication() {
        Application application = application(ApplicationStatus.APPLIED);
        Instant before = Instant.parse("2026-01-01T00:00:00Z");
        application.setUpdatedAt(before);

        assertThrows(BusinessException.class,
                () -> machine.transition(application, ApplicationStatus.ACCEPTED, ActorType.STUDENT, USER_ID, null));

        verify(history, never()).save(any(ApplicationStatusHistory.class));
        assertEquals(ApplicationStatus.APPLIED, application.getStatus());
        assertEquals(before, application.getUpdatedAt());
    }

    @Test
    void allowedChangeSetsStatusReasonAndUpdatedAt() {
        Application application = application(ApplicationStatus.INTERVIEW);
        Instant before = Instant.parse("2026-01-01T00:00:00Z");
        application.setUpdatedAt(before);

        machine.transition(application, ApplicationStatus.REJECTED, ActorType.SYSTEM, null,
                ApplicationStatusReason.POSITION_FILLED);

        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
        assertEquals(ApplicationStatusReason.POSITION_FILLED, application.getStatusReason());
        assertTrue(application.getUpdatedAt().isAfter(before));
    }

    @Test
    void historyRowOfAPersonsChangeKeepsTheirUserId() {
        machine.transition(application(ApplicationStatus.APPLIED), ApplicationStatus.REVIEWED, ActorType.EMPLOYER,
                USER_ID, null);

        ApplicationStatusHistory row = savedHistory();
        assertEquals(APPLICATION_ID, row.getApplicationId());
        assertEquals(ApplicationStatus.APPLIED, row.getFromStatus());
        assertEquals(ApplicationStatus.REVIEWED, row.getToStatus());
        assertEquals(ActorType.EMPLOYER, row.getActorType());
        assertEquals(USER_ID, row.getActorUserId());
        assertNull(row.getReason());
    }

    @Test
    void historyRowOfASystemChangeHasNoUserIdEvenIfOneIsPassed() {
        machine.transition(application(ApplicationStatus.OFFER), ApplicationStatus.REJECTED, ActorType.SYSTEM, USER_ID,
                ApplicationStatusReason.POSITION_FILLED);

        ApplicationStatusHistory row = savedHistory();
        assertEquals(ActorType.SYSTEM, row.getActorType());
        assertNull(row.getActorUserId());
        assertEquals(ApplicationStatusReason.POSITION_FILLED, row.getReason());
    }

    @Test
    void recordAppliedWritesTheFirstRowFromNothingToApplied() {
        machine.recordApplied(application(ApplicationStatus.APPLIED), USER_ID);

        ApplicationStatusHistory row = savedHistory();
        assertEquals(APPLICATION_ID, row.getApplicationId());
        assertNull(row.getFromStatus());
        assertEquals(ApplicationStatus.APPLIED, row.getToStatus());
        assertEquals(ActorType.STUDENT, row.getActorType());
        assertEquals(USER_ID, row.getActorUserId());
        assertNull(row.getReason());
    }

    /** Both writes must share the caller's transaction, so the status and its history never get out of step. */
    @Test
    void bothWritesNeedTheCallersTransaction() throws NoSuchMethodException {
        Method transition = ApplicationStateMachine.class.getMethod("transition", Application.class,
                ApplicationStatus.class, ActorType.class, Long.class, ApplicationStatusReason.class);
        Method recordApplied = ApplicationStateMachine.class.getMethod("recordApplied", Application.class, Long.class);

        assertNotNull(transition.getAnnotation(Transactional.class));
        assertEquals(Propagation.MANDATORY, transition.getAnnotation(Transactional.class).propagation());
        assertEquals(Propagation.MANDATORY, recordApplied.getAnnotation(Transactional.class).propagation());
    }

    private ApplicationStatusHistory savedHistory() {
        ArgumentCaptor<ApplicationStatusHistory> saved = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(history).save(saved.capture());
        return saved.getValue();
    }
}
