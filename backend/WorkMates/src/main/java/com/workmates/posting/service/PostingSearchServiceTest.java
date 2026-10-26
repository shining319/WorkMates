package com.workmates.posting.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.workmates.common.BusinessException;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.service.StudentProfileService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class PostingSearchServiceTest {
    private final PostingRepository repo = mock(PostingRepository.class);
    private final StudentProfileService students = mock(StudentProfileService.class);
    private final PostingSearchService service = new PostingSearchService(repo, students);

    private static Posting posting(long id, int availableHours) {
        Posting p = new Posting();
        p.setId(id);
        p.setEmployerId(6L);
        p.setTitle("Barista " + id);
        p.setJobType(JobType.PART_TIME);
        p.setStatus(PostingStatus.OPEN);
        p.setHourlyRate(new BigDecimal("14.00"));
        p.setWeeklyHours(new BigDecimal("4.0"));
        p.setArea(Area.ATHLONE);
        p.setAddress("Town centre");
        p.setHeadcount(1);
        p.setCreatedAt(Instant.now());
        p.getTimeSlots().add(new TimeSlot(3, 14 * 60, (14 + availableHours) * 60));
        return p;
    }

    @Test void timeCoverageFiltersBeforePaging() {
        // Job 1 is 4h and fits 3h (75%); job 2 is 8h and fits 3h (37.5%); job 3 fits 100%.
        var p1 = posting(1, 4);
        var p2 = posting(2, 8);
        var p3 = posting(3, 2);
        when(students.matchInput(7L)).thenReturn(new StudentMatchInput(
                List.of(new TimeSlot(3, 15 * 60, 18 * 60)), Set.of(), null));
        when(repo.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(p1, p2, p3));

        var firstPage = service.search(7L, null, List.of(), List.of(), null, true, 0, 1);
        var secondPage = service.search(7L, null, List.of(), List.of(), null, true, 1, 1);

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(Long.valueOf(1L), firstPage.getContent().get(0).getId());
        assertEquals(Long.valueOf(3L), secondPage.getContent().get(0).getId());
        verify(repo, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test void noFreeSlotsReturnsEmptyPageWithoutSearchingDatabase() {
        when(students.matchInput(7L)).thenReturn(new StudentMatchInput(List.of(), Set.of(), null));
        assertEquals(0, service.search(7L, null, null, null, null, true, 0, 20).getTotalElements());
        verifyNoInteractions(repo);
    }

    @Test void ordinaryFiltersUseDatabasePagination() {
        var p = posting(42, 4);
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        var response = service.search(7L, "barista", List.of(Area.ATHLONE),
                List.of(JobType.PART_TIME), new BigDecimal("14"), false, 0, 20);
        assertEquals(Long.valueOf(42L), response.getContent().get(0).getId());
        verifyNoInteractions(students);
    }

    @Test void negativePayAndInvalidPaginationAreRejected() {
        assertThrows(BusinessException.class, () -> service.search(7L, null, null, null,
                new BigDecimal("-1"), false, 0, 20));
        assertThrows(BusinessException.class, () -> service.search(7L, null, null, null,
                null, false, -1, 20));
        assertThrows(BusinessException.class, () -> service.search(7L, null, null, null,
                null, false, 0, 100));
        verifyNoInteractions(repo, students);
    }
}
