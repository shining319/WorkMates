package com.workmates.posting.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.workmates.application.dto.PostingCounts;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.dto.PostingDetailResponse;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.service.StudentProfileService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StudentPostingDetailServiceTest {
    private final PostingService postingService = mock(PostingService.class);
    private final PostingRepository postings = mock(PostingRepository.class);
    private final StudentProfileService profiles = mock(StudentProfileService.class);
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final StudentPostingDetailService detail = new StudentPostingDetailService(
            postingService, postings, profiles, applications);

    private Posting prepare(PostingStatus status) {
        Posting p = new Posting();
        p.setId(42L);
        p.setTitle("Barista");
        p.setEmployerId(5L);
        p.setHourlyRate(new BigDecimal("15"));
        p.setStatus(status);
        p.getTimeSlots().add(new TimeSlot(3, 840, 1080));
        EmployerProfile employer = new EmployerProfile();
        employer.setDisplayName("Café Uno");
        employer.setBio("Local café");
        when(postingService.get(42L)).thenReturn(PostingDetailResponse.from(p, employer, PostingCounts.empty(42L)));
        when(postings.findById(42L)).thenReturn(Optional.of(p));
        when(profiles.matchInput(7L)).thenReturn(new StudentMatchInput(
                List.of(new TimeSlot(3, 900, 1080)), Set.of(), new BigDecimal("14")));
        when(applications.findFirstByPostingIdAndStudentIdOrderByCreatedAtDescIdDesc(42L,7L))
                .thenReturn(Optional.empty());
        return p;
    }

    @Test void openPostingGivesBreakdownAndApplyOption() {
        prepare(PostingStatus.OPEN);
        var result = detail.get(7L, 42L);
        assertEquals("Café Uno", result.getEmployerName());
        assertEquals(75, result.getMatchBreakdown().timePercent());
        assertEquals("UNCOVERED", result.getMatchBreakdown().jobSlotCoverage().get(0)
                .uncovered().get(0).coverage());
        assertEquals("15:00", result.getStudentFreeSlots().get(0).getStart());
        assertTrue(result.getCanApply());
        assertFalse(result.getAlreadyApplied());
        assertNull(result.getApplicationId());
    }

    @Test void closedPostingIsClearlyMarkedAndCannotApply() {
        prepare(PostingStatus.CLOSED);
        var result = detail.get(7L, 42L);
        assertEquals("This posting is closed", result.getClosedNotice());
        assertFalse(result.getCanApply());
    }

    @Test void existingApplicationDisplaysStatusAndHidesApply() {
        prepare(PostingStatus.OPEN);
        Application application = new Application();
        application.setId(9L);
        application.setStatus(ApplicationStatus.REVIEWED);
        when(applications.findFirstByPostingIdAndStudentIdOrderByCreatedAtDescIdDesc(42L,7L))
                .thenReturn(Optional.of(application));
        var result = detail.get(7L, 42L);
        assertEquals(9L, result.getApplicationId());
        assertEquals(ApplicationStatus.REVIEWED, result.getApplicationStatus());
        assertTrue(result.getAlreadyApplied());
        assertFalse(result.getCanApply());
    }
}
