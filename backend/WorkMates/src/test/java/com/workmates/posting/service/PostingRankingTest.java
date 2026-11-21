package com.workmates.posting.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.repository.EmployerProfileRepository;
import com.workmates.profile.service.StudentProfileService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class PostingRankingTest {
    private final PostingRepository postings = mock(PostingRepository.class);
    private final EmployerProfileRepository employers = mock(EmployerProfileRepository.class);
    private final StudentProfileService students = mock(StudentProfileService.class);
    private final PostingSearchService search = new PostingSearchService(postings, employers, students);
    private static final PostingSearchService.Filters NO_FILTERS =
            new PostingSearchService.Filters(null, null, null, null, false);

    private static Posting job(long id, String rate, int start, Instant created) {
        Posting p = new Posting();
        p.setId(id);
        p.setTitle("Job " + id);
        p.setHourlyRate(new BigDecimal(rate));
        p.setEmployerId(100L);
        p.setStatus(PostingStatus.OPEN);
        p.setCreatedAt(created);
        p.getTimeSlots().add(new TimeSlot(3, start, start + 120));
        return p;
    }

    @SuppressWarnings("unchecked")
    @Test void ranksByUnroundedScoreThenNewestAndReturnsMatchPercent() {
        Posting oldFull = job(1, "20", 900, Instant.parse("2026-10-01T00:00:00Z"));
        Posting newFull = job(2, "20", 900, Instant.parse("2026-10-02T00:00:00Z"));
        Posting low = job(3, "10", 1200, Instant.parse("2026-10-03T00:00:00Z"));
        when(postings.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(low, oldFull, newFull));
        when(students.matchInput(7L)).thenReturn(new StudentMatchInput(
                List.of(new TimeSlot(3, 900, 1020)), Set.of(), new BigDecimal("10")));
        when(employers.findAllById(any())).thenReturn(List.of());
        var page = search.search(7L, NO_FILTERS, 0);
        assertEquals(List.of(2L, 1L, 3L), page.getContent().stream().map(c -> c.getId()).toList());
        assertEquals(67, page.getContent().get(0).getMatchPercent());
        assertEquals(33, page.getContent().get(2).getMatchPercent());
    }

    @SuppressWarnings("unchecked")
    @Test void profileChangesAreUsedAtNextRequest() {
        Posting first = job(1, "10", 900, Instant.parse("2026-10-01T00:00:00Z"));
        Posting second = job(2, "10", 1200, Instant.parse("2026-10-02T00:00:00Z"));
        when(postings.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(first, second));
        when(employers.findAllById(any())).thenReturn(List.of());
        when(students.matchInput(7L))
                .thenReturn(new StudentMatchInput(List.of(new TimeSlot(3,900,1020)), Set.of(), null))
                .thenReturn(new StudentMatchInput(List.of(new TimeSlot(3,1200,1320)), Set.of(), null));
        assertEquals(1L, search.search(7L, NO_FILTERS, 0).getContent().get(0).getId());
        assertEquals(2L, search.search(7L, NO_FILTERS, 0).getContent().get(0).getId());
    }
    @SuppressWarnings("unchecked")
    @Test void ordersByExactScoreEvenWhenDisplayedPercentIsTheSame() {
        Posting slightlyBetter = job(1, "19.99", 900, Instant.parse("2026-10-01T00:00:00Z"));
        Posting slightlyWorseNewer = job(2, "19.98", 900, Instant.parse("2026-11-01T00:00:00Z"));
        when(postings.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(slightlyWorseNewer, slightlyBetter));
        when(students.matchInput(7L)).thenReturn(new StudentMatchInput(
                List.of(new TimeSlot(3,900,1020)), Set.of(), new BigDecimal("20")));
        when(employers.findAllById(any())).thenReturn(List.of());
        var cards = search.search(7L, NO_FILTERS, 0).getContent();
        assertEquals(cards.get(0).getMatchPercent(), cards.get(1).getMatchPercent());
        assertEquals(1L, cards.get(0).getId()); // exact score wins over newer timestamp
    }

}
