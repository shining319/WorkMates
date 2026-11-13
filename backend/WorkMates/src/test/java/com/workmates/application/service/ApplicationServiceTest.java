package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.workmates.application.dto.ApplicationResponse;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.service.NotificationService;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.service.PostingService;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.service.StudentProfileService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

class ApplicationServiceTest {
    private static final Long STUDENT_ID = 3L;
    private static final Long EMPLOYER_ID = 5L;
    private static final Long POSTING_ID = 42L;
    private static final Long APPLICATION_ID = 99L;

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationStateMachine stateMachine = mock(ApplicationStateMachine.class);
    private final PostingService postings = mock(PostingService.class);
    private final StudentProfileService profiles = mock(StudentProfileService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ApplicationService service =
            new ApplicationService(applications, stateMachine, postings, profiles, notifications);

    @BeforeEach
    void setUp() {
        when(postings.lockPosting(POSTING_ID)).thenReturn(posting(PostingStatus.OPEN));
        when(profiles.get(STUDENT_ID)).thenReturn(profile(List.of()));
        when(applications.saveAndFlush(any(Application.class))).thenAnswer(inv -> {
            Application saved = inv.getArgument(0);
            saved.setId(APPLICATION_ID);
            return saved;
        });
    }

    private static Posting posting(PostingStatus status) {
        Posting posting = new Posting();
        posting.setId(POSTING_ID);
        posting.setEmployerId(EMPLOYER_ID);
        posting.setTitle("Barista");
        posting.setStatus(status);
        return posting;
    }

    private static StudentProfileResponse profile(List<String> missing) {
        StudentProfileResponse profile = new StudentProfileResponse();
        profile.setName("Ann Murphy");
        profile.setComplete(missing.isEmpty());
        profile.setMissing(missing);
        return profile;
    }

    private void assertNothingWritten() {
        verify(applications, never()).saveAndFlush(any(Application.class));
        verifyNoInteractions(stateMachine, notifications);
    }

    @Test
    void createsAnAppliedApplicationForTheStudent() {
        ApplicationResponse response = service.apply(STUDENT_ID, POSTING_ID);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applications).saveAndFlush(saved.capture());
        assertEquals(POSTING_ID, saved.getValue().getPostingId());
        assertEquals(STUDENT_ID, saved.getValue().getStudentId());
        assertEquals(ApplicationStatus.APPLIED, saved.getValue().getStatus());
        assertEquals(APPLICATION_ID, response.getId());
        assertEquals(POSTING_ID, response.getPostingId());
        assertEquals(ApplicationStatus.APPLIED, response.getStatus());
    }

    @Test
    void writesTheFirstHistoryRowForTheNewApplication() {
        service.apply(STUDENT_ID, POSTING_ID);

        ArgumentCaptor<Application> recorded = ArgumentCaptor.forClass(Application.class);
        verify(stateMachine).recordApplied(recorded.capture(), eq(STUDENT_ID));
        assertEquals(APPLICATION_ID, recorded.getValue().getId());
    }

    @Test
    void tellsOnlyTheEmployerAndLinksToTheApplication() {
        service.apply(STUDENT_ID, POSTING_ID);

        verify(notifications).notify(EMPLOYER_ID, NotificationType.APPLICATION_RECEIVED,
                "Ann Murphy applied for Barista", NotificationTargetType.APPLICATION, APPLICATION_ID);
        // The student, who made the change, gets nothing
        verify(notifications, never()).notify(eq(STUDENT_ID), any(), any(), any(), anyLong());
    }

    @Test
    void locksThePostingBeforeAnyCheckOrWrite() {
        service.apply(STUDENT_ID, POSTING_ID);

        InOrder order = inOrder(postings, profiles, applications);
        order.verify(postings).lockPosting(POSTING_ID);
        order.verify(profiles).get(STUDENT_ID);
        order.verify(applications).existsByPostingIdAndStudentId(POSTING_ID, STUDENT_ID);
        order.verify(applications).saveAndFlush(any(Application.class));
    }

    @Test
    void missingPostingIsNotFound() {
        when(postings.lockPosting(POSTING_ID)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(STUDENT_ID, POSTING_ID));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
        assertNothingWritten();
    }

    @Test
    void closedPostingIsRefused() {
        when(postings.lockPosting(POSTING_ID)).thenReturn(posting(PostingStatus.CLOSED));

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(STUDENT_ID, POSTING_ID));

        assertEquals(ErrorCode.POSTING_CLOSED, e.getCode());
        assertNothingWritten();
    }

    @Test
    void closedPostingIsRefusedBeforeTheProfileIsChecked() {
        when(postings.lockPosting(POSTING_ID)).thenReturn(posting(PostingStatus.CLOSED));
        when(profiles.get(STUDENT_ID)).thenReturn(profile(List.of("tags")));

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(STUDENT_ID, POSTING_ID));

        assertEquals(ErrorCode.POSTING_CLOSED, e.getCode());
    }

    @Test
    void incompleteProfileIsRefusedAndTheMessageListsWhatIsMissing() {
        when(profiles.get(STUDENT_ID)).thenReturn(profile(List.of("tags", "timeSlots")));

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(STUDENT_ID, POSTING_ID));

        assertEquals(ErrorCode.PROFILE_INCOMPLETE, e.getCode());
        assertEquals("Complete your profile before applying (missing: tags, timeSlots)", e.getMessage());
        assertNothingWritten();
    }

    @Test
    void secondApplicationIsRefusedByTheEarlierCheck() {
        when(applications.existsByPostingIdAndStudentId(POSTING_ID, STUDENT_ID)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(STUDENT_ID, POSTING_ID));

        assertEquals(ErrorCode.ALREADY_APPLIED, e.getCode());
        assertNothingWritten();
    }

    @Test
    void uniqueIndexClashIsAlsoReportedAsAlreadyApplied() {
        when(applications.saveAndFlush(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException("uk_applications_posting_student"));

        BusinessException e = assertThrows(BusinessException.class, () -> service.apply(STUDENT_ID, POSTING_ID));

        assertEquals(ErrorCode.ALREADY_APPLIED, e.getCode());
        verifyNoInteractions(stateMachine, notifications);
    }
}
