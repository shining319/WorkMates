package com.workmates.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.application.dto.PostingCounts;
import com.workmates.application.repository.ApplicationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationQueryServiceTest {
    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final ApplicationQueryService service = new ApplicationQueryService(repository);

    @Test
    void returnsTheCountsFoundForEachPosting() {
        PostingCounts one = new PostingCounts(1L, 2L, 3L, 4L);
        PostingCounts two = new PostingCounts(2L, 0L, 5L, 0L);
        when(repository.countByPostings(List.of(1L, 2L))).thenReturn(List.of(one, two));

        Map<Long, PostingCounts> counts = service.countsByPostings(List.of(1L, 2L));

        assertEquals(one, counts.get(1L));
        assertEquals(two, counts.get(2L));
    }

    @Test
    void aPostingWithoutApplicationsHasZeroCounts() {
        when(repository.countByPostings(List.of(1L, 2L)))
                .thenReturn(List.of(new PostingCounts(1L, 1L, 1L, 1L)));

        Map<Long, PostingCounts> counts = service.countsByPostings(List.of(1L, 2L));

        assertEquals(PostingCounts.empty(2L), counts.get(2L));
        assertEquals(0L, counts.get(2L).hired());
        assertEquals(0L, counts.get(2L).inProgress());
        assertEquals(0L, counts.get(2L).autoRejected());
    }

    @Test
    void noIdsMeansNoQuery() {
        assertEquals(Map.of(), service.countsByPostings(List.of()));

        verify(repository, never()).countByPostings(any());
    }

    @Test
    void countsOfReturnsTheCountsOfOnePosting() {
        when(repository.countByPostings(List.of(7L))).thenReturn(List.of(new PostingCounts(7L, 2L, 1L, 0L)));

        assertEquals(new PostingCounts(7L, 2L, 1L, 0L), service.countsOf(7L));
    }
}
