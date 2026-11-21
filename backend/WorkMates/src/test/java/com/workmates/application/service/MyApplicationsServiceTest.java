package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.application.dto.MyApplicationResponse;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationGroup;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class MyApplicationsServiceTest {
    private static final Long STUDENT_ID = 9L;

    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final PostingRepository postings = mock(PostingRepository.class);
    private final EmployerProfileRepository employers = mock(EmployerProfileRepository.class);
    private final MyApplicationsService service = new MyApplicationsService(applications, postings, employers);

    private static Application application(long id, long postingId, ApplicationStatus status) {
        Application a = new Application();
        a.setId(id);
        a.setPostingId(postingId);
        a.setStudentId(STUDENT_ID);
        a.setStatus(status);
        return a;
    }

    private static Posting posting(long id, long employerId, String title) {
        Posting p = new Posting();
        p.setId(id);
        p.setEmployerId(employerId);
        p.setTitle(title);
        return p;
    }

    private static EmployerProfile employer(long userId, String name) {
        EmployerProfile e = new EmployerProfile();
        e.setUserId(userId);
        e.setDisplayName(name);
        return e;
    }

    private static Page<Application> page(Application... items) {
        return new PageImpl<>(List.of(items));
    }

    private Pageable capturedPageable(ArgumentCaptor<Pageable> captor) {
        return captor.getValue();
    }

    @Test
    void noGroupListsEveryStatus() {
        when(applications.findByStudentId(eq(STUDENT_ID), any())).thenReturn(page());

        service.mine(STUDENT_ID, null, 0);

        verify(applications).findByStudentId(eq(STUDENT_ID), any());
        verify(applications, never()).findByStudentIdAndStatusIn(any(), any(), any());
    }

    @Test
    void groupsMapToTheDocumentedStatuses() {
        assertEquals(List.of(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWED, ApplicationStatus.INTERVIEW),
                ApplicationGroup.IN_PROGRESS.getStatuses());
        assertEquals(List.of(ApplicationStatus.OFFER), ApplicationGroup.OFFER.getStatuses());
        assertEquals(List.of(ApplicationStatus.ACCEPTED), ApplicationGroup.ACCEPTED.getStatuses());
        assertEquals(List.of(ApplicationStatus.REJECTED, ApplicationStatus.DECLINED, ApplicationStatus.COMPLETED),
                ApplicationGroup.FINISHED.getStatuses());
    }

    @Test
    void aGroupFiltersByItsStatusesAndOnlyForTheStudent() {
        when(applications.findByStudentIdAndStatusIn(eq(STUDENT_ID), any(), any())).thenReturn(page());

        service.mine(STUDENT_ID, ApplicationGroup.FINISHED, 0);

        verify(applications).findByStudentIdAndStatusIn(eq(STUDENT_ID),
                eq(List.of(ApplicationStatus.REJECTED, ApplicationStatus.DECLINED, ApplicationStatus.COMPLETED)),
                any());
    }

    @Test
    void sortsByLastUpdateNewestFirstWithTwentyPerPage() {
        when(applications.findByStudentId(eq(STUDENT_ID), any())).thenReturn(page());

        service.mine(STUDENT_ID, null, 2);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(applications).findByStudentId(eq(STUDENT_ID), captor.capture());
        Pageable pageable = capturedPageable(captor);
        assertEquals(PageRequest.of(2, 20, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))), pageable);
    }

    @Test
    void rowsCarryPostingTitleEmployerNameAndReason() {
        Application filled = application(1, 100, ApplicationStatus.REJECTED);
        filled.setStatusReason(ApplicationStatusReason.POSITION_FILLED);
        Application applied = application(2, 101, ApplicationStatus.APPLIED);
        when(applications.findByStudentId(eq(STUDENT_ID), any())).thenReturn(page(filled, applied));
        when(postings.findAllById(any())).thenReturn(List.of(posting(100, 5, "Barista"), posting(101, 6, "Tutor")));
        when(employers.findAllById(any())).thenReturn(List.of(employer(5, "Riverside Cafe"), employer(6, "Campus Hub")));

        List<MyApplicationResponse> rows = service.mine(STUDENT_ID, null, 0).getContent();

        assertEquals(2, rows.size());
        assertEquals("Barista", rows.get(0).getPostingTitle());
        assertEquals("Riverside Cafe", rows.get(0).getEmployerName());
        assertEquals(ApplicationStatus.REJECTED, rows.get(0).getStatus());
        assertEquals(ApplicationStatusReason.POSITION_FILLED, rows.get(0).getStatusReason());
        assertEquals("Tutor", rows.get(1).getPostingTitle());
        assertEquals("Campus Hub", rows.get(1).getEmployerName());
        assertNull(rows.get(1).getStatusReason());
    }

    @Test
    void noApplicationsGivesAnEmptyPage() {
        when(applications.findByStudentId(eq(STUDENT_ID), any())).thenReturn(page());

        Page<MyApplicationResponse> result = service.mine(STUDENT_ID, null, 0);

        assertEquals(0, result.getTotalElements());
        assertEquals(List.of(), result.getContent());
    }

    @Test
    void negativePageIsRejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.mine(STUDENT_ID, null, -1));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
        verify(applications, never()).findByStudentId(any(), any());
    }
}
