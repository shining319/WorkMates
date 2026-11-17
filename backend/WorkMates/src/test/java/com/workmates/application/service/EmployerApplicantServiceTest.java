package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.account.entity.User;
import com.workmates.account.repository.UserRepository;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.service.StudentProfileService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployerApplicantServiceTest {
    private final PostingRepository postings = mock(PostingRepository.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StudentProfileService students = mock(StudentProfileService.class);
    private final EmployerApplicantService service = new EmployerApplicantService(postings, applications, users, students);
    private Posting job;

    @BeforeEach
    void setup() {
        job = new Posting();
        job.setId(7L);
        job.setEmployerId(20L);
        job.setHourlyRate(new BigDecimal("15.00"));
        job.setTags(new HashSet<>(Set.of(new Tag("english"))));
        job.setTimeSlots(new ArrayList<>(List.of(new TimeSlot(6, 540, 1020))));
        when(postings.findById(7L)).thenReturn(Optional.of(job));
    }

    @Test
    void neverRevealsApplicantsToAnotherEmployer() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.list(99L, 7L, null, "matchScore", "desc", 0));
        assertEquals(ErrorCode.NOT_FOUND, error.getCode());
        verify(applications, never()).findByPostingId(any());
    }

    @Test
    void returnsEmptyPageForNoApplicantsWithoutLoadingStudentProfiles() {
        when(applications.findByPostingId(7L)).thenReturn(List.of());
        var page = service.list(20L, 7L, null, "matchScore", "desc", 0);
        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalElements());
        assertEquals(20, page.getSize());
        verify(students, never()).matchInput(any());
    }

    @Test
    void filtersStatusThenRanksByLiveMatchAndNotByNewerApplicationDate() {
        Application best = application(101L, 1L, ApplicationStatus.APPLIED, "2026-11-01T10:00:00Z");
        Application low = application(102L, 2L, ApplicationStatus.APPLIED, "2026-11-03T10:00:00Z");
        Application excluded = application(103L, 3L, ApplicationStatus.REJECTED, "2026-11-04T10:00:00Z");
        when(applications.findByPostingId(7L)).thenReturn(List.of(low, excluded, best));
        when(users.findAllById(List.of(2L, 1L))).thenReturn(List.of(user(1L), user(2L)));
        when(students.matchInput(1L)).thenReturn(input(true));
        when(students.matchInput(2L)).thenReturn(input(false));

        var result = service.list(20L, 7L, ApplicationStatus.APPLIED, "matchScore", "desc", 0);

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(101L, 102L), result.getContent().stream().map(row -> row.applicationId()).toList());
        assertEquals(100, result.getContent().get(0).matchPercent());
        assertEquals(0, result.getContent().get(1).matchPercent());
        verify(students, never()).matchInput(3L);
    }

    @Test
    void sortsByApplicationDateAscendingWhenRequested() {
        Application first = application(101L, 1L, ApplicationStatus.APPLIED, "2026-11-01T10:00:00Z");
        Application second = application(102L, 2L, ApplicationStatus.OFFER, "2026-11-03T10:00:00Z");
        when(applications.findByPostingId(7L)).thenReturn(List.of(second, first));
        when(users.findAllById(List.of(2L, 1L))).thenReturn(List.of(user(1L), user(2L)));
        when(students.matchInput(any())).thenReturn(input(true));

        var result = service.list(20L, 7L, null, "applicationDate", "asc", 0);
        assertEquals(List.of(101L, 102L), result.getContent().stream().map(row -> row.applicationId()).toList());
    }

    @Test
    void paginationUsesTwentyRowsAndReportsFullCount() {
        List<Application> all = LongStream.rangeClosed(1, 25)
                .mapToObj(i -> application(100L + i, i, ApplicationStatus.APPLIED,
                        Instant.parse("2026-11-01T00:00:00Z").plusSeconds(i).toString()))
                .toList();
        when(applications.findByPostingId(7L)).thenReturn(all);
        when(users.findAllById(any())).thenReturn(LongStream.rangeClosed(1, 25).mapToObj(this::user).toList());
        when(students.matchInput(any())).thenReturn(input(true));

        var first = service.list(20L, 7L, null, "applicationDate", "desc", 0);
        var second = service.list(20L, 7L, null, "applicationDate", "desc", 1);
        assertEquals(20, first.getContent().size());
        assertEquals(5, second.getContent().size());
        assertEquals(25, second.getTotalElements());
        assertEquals(2, second.getTotalPages());
        assertEquals(125L, first.getContent().get(0).applicationId());
        assertEquals(101L, second.getContent().get(4).applicationId());
    }

    @Test
    void detailReturnsCurrentProfileAndMatchExplanation() {
        Application application = application(101L, 1L, ApplicationStatus.REVIEWED, "2026-11-01T10:00:00Z");
        when(applications.findById(101L)).thenReturn(Optional.of(application));
        when(users.findById(1L)).thenReturn(Optional.of(user(1L)));
        StudentProfileResponse profile = new StudentProfileResponse();
        profile.setPhone("0871234567");
        profile.setExpectedHourlyRate(new BigDecimal("14.00"));
        when(students.get(1L)).thenReturn(profile);
        when(students.matchInput(1L)).thenReturn(input(true));

        var result = service.detail(20L, 7L, 101L);
        assertEquals("student1@student.tus.ie", result.campusEmail());
        assertEquals("0871234567", result.phone());
        assertEquals(ApplicationStatus.REVIEWED, result.status());
        assertEquals(100, result.matchBreakdown().overallPercent());
        assertNotNull(result.profile());
        assertEquals(1, result.matchBreakdown().jobSlotCoverage().size());
    }

    @Test
    void applicantFromAnotherPostingIsNotVisible() {
        when(applications.findById(101L)).thenReturn(Optional.of(
                application(101L, 1L, ApplicationStatus.APPLIED, "2026-11-01T10:00:00Z")));
        // The employer owns posting 7, but this application belongs to posting 9.
        Application unrelated = application(101L, 1L, ApplicationStatus.APPLIED, "2026-11-01T10:00:00Z");
        unrelated.setPostingId(9L);
        when(applications.findById(101L)).thenReturn(Optional.of(unrelated));
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.detail(20L, 7L, 101L));
        assertEquals(ErrorCode.NOT_FOUND, error.getCode());
    }

    @Test
    void invalidSortingIsRejected() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.list(20L, 7L, null, "unknown", "desc", 0));
        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verify(applications, never()).findByPostingId(any());
    }

    private Application application(Long id, Long studentId, ApplicationStatus status, String date) {
        Application app = new Application();
        app.setId(id);
        app.setPostingId(7L);
        app.setStudentId(studentId);
        app.setStatus(status);
        app.setCreatedAt(Instant.parse(date));
        return app;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Student " + id);
        user.setEmail("student" + id + "@student.tus.ie");
        return user;
    }

    private StudentMatchInput input(boolean matches) {
        return matches
                ? new StudentMatchInput(List.of(new TimeSlot(6, 540, 1020)), Set.of(),
                        new BigDecimal("14.00"), List.of("english"))
                : new StudentMatchInput(List.of(), Set.of(), null, List.of());
    }
}
