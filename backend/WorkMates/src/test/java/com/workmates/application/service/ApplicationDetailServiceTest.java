package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.application.dto.ApplicationDetailResponse;
import com.workmates.application.dto.ApplicationDetailResponse.HistoryStep;
import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusHistory;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.application.repository.ApplicationStatusHistoryRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.Role;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.EmployerProfileResponse;
import com.workmates.profile.service.EmployerProfileService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ApplicationDetailServiceTest {
    private static final Long APPLICATION_ID = 7L;
    private static final Long POSTING_ID = 42L;
    private static final Long STUDENT_ID = 9L;
    private static final Long EMPLOYER_ID = 20L;
    private static final CurrentUser STUDENT = new CurrentUser(STUDENT_ID, Role.STUDENT);
    private static final CurrentUser EMPLOYER = new CurrentUser(EMPLOYER_ID, Role.EMPLOYER);

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final ApplicationStatusHistoryRepository history = mock(ApplicationStatusHistoryRepository.class);
    private final PostingRepository postings = mock(PostingRepository.class);
    private final EmployerProfileService employers = mock(EmployerProfileService.class);
    private final ApplicationDetailService service =
            new ApplicationDetailService(applications, history, postings, employers);

    @BeforeEach
    void setUp() {
        Application application = new Application();
        application.setId(APPLICATION_ID);
        application.setPostingId(POSTING_ID);
        application.setStudentId(STUDENT_ID);
        application.setStatus(ApplicationStatus.REJECTED);
        application.setStatusReason(ApplicationStatusReason.POSITION_FILLED);
        application.setCreatedAt(Instant.parse("2026-11-01T10:00:00Z"));
        application.setUpdatedAt(Instant.parse("2026-11-05T10:00:00Z"));
        when(applications.findById(APPLICATION_ID)).thenReturn(Optional.of(application));

        Posting posting = new Posting();
        posting.setId(POSTING_ID);
        posting.setEmployerId(EMPLOYER_ID);
        posting.setTitle("Summer Barista");
        when(postings.findById(POSTING_ID)).thenReturn(Optional.of(posting));

        when(history.findByApplicationIdOrderByCreatedAtAscIdAsc(APPLICATION_ID)).thenReturn(List.of(
                step(null, ApplicationStatus.APPLIED, ActorType.STUDENT, null, "2026-11-01T10:00:00Z"),
                step(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED, ActorType.SYSTEM,
                        ApplicationStatusReason.POSITION_FILLED, "2026-11-05T10:00:00Z")));

        EmployerProfileResponse employer = new EmployerProfileResponse();
        employer.setDisplayName("Lakeside Cafe");
        employer.setContactEmail("lakeside.cafe@example.com");
        employer.setPhone("0906401234");
        when(employers.get(EMPLOYER_ID)).thenReturn(employer);
    }

    private static ApplicationStatusHistory step(ApplicationStatus from, ApplicationStatus to, ActorType actor,
                                                 ApplicationStatusReason reason, String time) {
        ApplicationStatusHistory step = new ApplicationStatusHistory(APPLICATION_ID, from, to, actor,
                actor == ActorType.SYSTEM ? null : STUDENT_ID, reason);
        ReflectionTestUtils.setField(step, "createdAt", Instant.parse(time));
        return step;
    }

    @Test
    void studentSeesOwnApplicationWithEmployerContact() {
        ApplicationDetailResponse detail = service.get(STUDENT, APPLICATION_ID);

        assertEquals(APPLICATION_ID, detail.id());
        assertEquals(POSTING_ID, detail.postingId());
        assertEquals("Summer Barista", detail.postingTitle());
        assertEquals("Lakeside Cafe", detail.employerContact().name());
        assertEquals("lakeside.cafe@example.com", detail.employerContact().contactEmail());
        assertEquals("0906401234", detail.employerContact().phone());
    }

    @Test
    void employerSeesApplicationToOwnPostingWithoutEmployerContact() {
        ApplicationDetailResponse detail = service.get(EMPLOYER, APPLICATION_ID);

        assertEquals(POSTING_ID, detail.postingId());
        assertEquals(2, detail.history().size());
        assertNull(detail.employerContact());
        verify(employers, never()).get(any());
    }

    @Test
    void statusReasonAndHistoryAreReturnedOldestFirst() {
        ApplicationDetailResponse detail = service.get(STUDENT, APPLICATION_ID);

        assertEquals(ApplicationStatus.REJECTED, detail.status());
        assertEquals(ApplicationStatusReason.POSITION_FILLED, detail.statusReason());
        assertEquals(Instant.parse("2026-11-01T10:00:00Z"), detail.createdAt());
        assertEquals(Instant.parse("2026-11-05T10:00:00Z"), detail.updatedAt());
        HistoryStep first = detail.history().get(0);
        assertNull(first.fromStatus());
        assertEquals(ApplicationStatus.APPLIED, first.toStatus());
        assertEquals(ActorType.STUDENT, first.actorType());
        HistoryStep last = detail.history().get(1);
        assertEquals(ApplicationStatus.APPLIED, last.fromStatus());
        assertEquals(ApplicationStatus.REJECTED, last.toStatus());
        assertEquals(ActorType.SYSTEM, last.actorType());
        assertEquals(ApplicationStatusReason.POSITION_FILLED, last.reason());
        assertEquals(Instant.parse("2026-11-05T10:00:00Z"), last.createdAt());
    }

    @Test
    void anotherStudentGetsNotFound() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.get(new CurrentUser(99L, Role.STUDENT), APPLICATION_ID));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
        verify(history, never()).findByApplicationIdOrderByCreatedAtAscIdAsc(any());
    }

    @Test
    void employerOfAnotherPostingGetsNotFound() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.get(new CurrentUser(99L, Role.EMPLOYER), APPLICATION_ID));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
        verify(history, never()).findByApplicationIdOrderByCreatedAtAscIdAsc(any());
    }

    @Test
    void studentIdUsedAsEmployerIdDoesNotGrantAccess() {
        // An employer whose user ID equals the student ID must not pass the student's ownership check
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.get(new CurrentUser(STUDENT_ID, Role.EMPLOYER), APPLICATION_ID));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
    }

    @Test
    void missingApplicationGetsNotFound() {
        when(applications.findById(8L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.get(STUDENT, 8L));

        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
    }
}
