package com.workmates.posting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.dto.JobCardResponse;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.posting.service.PostingSearchService.Filters;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import com.workmates.profile.service.StudentProfileService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@SuppressWarnings("unchecked")
class PostingSearchServiceTest {
    private static final Long STUDENT_ID = 7L;
    private static final int WED = DayOfWeek.WEDNESDAY.getValue();
    private static final Filters NO_FILTERS = new Filters(null, null, null, null, false);

    private final PostingRepository postings = mock(PostingRepository.class);
    private final EmployerProfileRepository employers = mock(EmployerProfileRepository.class);
    private final StudentProfileService students = mock(StudentProfileService.class);
    private final PostingSearchService service = new PostingSearchService(postings, employers, students);

    @BeforeEach
    void setUp() {
        when(employers.findAllById(any())).thenReturn(List.of(employer(5L, "Café Uno")));
    }

    private static EmployerProfile employer(Long id, String displayName) {
        EmployerProfile employer = new EmployerProfile();
        employer.setUserId(id);
        employer.setDisplayName(displayName);
        return employer;
    }

    private static TimeSlot slot(int startHour, int endHour) {
        return new TimeSlot(WED, startHour * 60, endHour * 60);
    }

    private static Posting posting(long id, TimeSlot... slots) {
        Posting posting = new Posting();
        posting.setId(id);
        posting.setEmployerId(5L);
        posting.setTitle("Job " + id);
        posting.setJobType(JobType.PART_TIME);
        posting.setHourlyRate(new BigDecimal("13.50"));
        posting.setWeeklyHours(new BigDecimal("8.0"));
        posting.setArea(Area.ATHLONE);
        posting.setStatus(PostingStatus.OPEN);
        posting.getTimeSlots().addAll(List.of(slots));
        return posting;
    }

    private void repositoryReturns(List<Posting> found) {
        when(postings.findAll(ArgumentMatchers.<Specification<Posting>>any(), any(Sort.class))).thenReturn(found);
    }

    private void studentIsFreeAt(TimeSlot... slots) {
        when(students.matchInput(STUDENT_ID)).thenReturn(new StudentMatchInput(List.of(slots), Set.of(), null));
    }

    private List<Long> ids(Page<JobCardResponse> page) {
        return page.getContent().stream().map(JobCardResponse::getId).toList();
    }

    /** Runs the specification that the service gave to the repository against mocks, and returns the mocks. */
    private Mocks runSpecification(Filters filters) {
        repositoryReturns(List.of());
        service.search(STUDENT_ID, filters, 0);
        ArgumentCaptor<Specification<Posting>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(postings).findAll(captor.capture(), any(Sort.class));
        Mocks mocks = new Mocks();
        captor.getValue().toPredicate(mocks.root, mocks.query, mocks.cb);
        return mocks;
    }

    private static class Mocks {
        final Root<Posting> root = mock(Root.class, RETURNS_DEEP_STUBS);
        final CriteriaQuery<?> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
        final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    }

    // --- cards, order, employer names ---

    @Test
    void returnsCardsInTheOrderOfTheRepositoryWithEmployerName() {
        repositoryReturns(List.of(posting(3), posting(2), posting(1)));

        Page<JobCardResponse> page = service.search(STUDENT_ID, NO_FILTERS, 0);

        assertEquals(List.of(3L, 2L, 1L), ids(page));
        JobCardResponse card = page.getContent().get(0);
        assertEquals("Job 3", card.getTitle());
        assertEquals("Café Uno", card.getEmployerName());
        assertEquals(JobType.PART_TIME, card.getJobType());
        assertEquals(new BigDecimal("13.50"), card.getHourlyRate());
        assertEquals(new BigDecimal("8.0"), card.getWeeklyHours());
        assertEquals(Area.ATHLONE, card.getArea());
    }

    @Test
    void asksTheRepositoryForNewestFirst() {
        repositoryReturns(List.of());

        service.search(STUDENT_ID, NO_FILTERS, 0);

        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(postings).findAll(ArgumentMatchers.<Specification<Posting>>any(), sort.capture());
        assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")), sort.getValue());
    }

    @Test
    void doesNotReadTheStudentProfileUnlessFitsMyTimeIsOn() {
        repositoryReturns(List.of(posting(1)));

        service.search(STUDENT_ID, NO_FILTERS, 0);

        verify(students, never()).matchInput(any());
    }

    @Test
    void asksForTheEmployersOfThePageOnly() {
        List<Posting> found = new ArrayList<>();
        for (long id = 1; id <= 25; id++) {
            Posting posting = posting(id);
            posting.setEmployerId(id <= 20 ? 5L : 6L); // employer 6 owns postings that are only on page 1
            found.add(posting);
        }
        repositoryReturns(found);

        service.search(STUDENT_ID, NO_FILTERS, 0);

        verify(employers).findAllById(List.of(5L));
    }

    // --- paging ---

    @Test
    void pagesAreTwentyLongAndTheLastOneHoldsTheRest() {
        List<Posting> found = new ArrayList<>();
        for (long id = 45; id >= 1; id--) {
            found.add(posting(id));
        }
        repositoryReturns(found);

        Page<JobCardResponse> first = service.search(STUDENT_ID, NO_FILTERS, 0);
        Page<JobCardResponse> last = service.search(STUDENT_ID, NO_FILTERS, 2);

        assertEquals(20, first.getContent().size());
        assertEquals(45L, first.getContent().get(0).getId());
        assertEquals(45, first.getTotalElements());
        assertEquals(3, first.getTotalPages());
        assertEquals(20, first.getSize());
        assertEquals(5, last.getContent().size());
        assertEquals(5L, last.getContent().get(0).getId());
        assertEquals(1L, last.getContent().get(4).getId());
    }

    @Test
    void exactlyTwentyResultsFillOnePage() {
        List<Posting> found = new ArrayList<>();
        for (long id = 20; id >= 1; id--) {
            found.add(posting(id));
        }
        repositoryReturns(found);

        Page<JobCardResponse> page = service.search(STUDENT_ID, NO_FILTERS, 0);

        assertEquals(20, page.getContent().size());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, service.search(STUDENT_ID, NO_FILTERS, 1).getContent().size());
    }

    @Test
    void aPageBeyondTheEndIsEmptyButKeepsTheTotal() {
        repositoryReturns(List.of(posting(1), posting(2)));

        Page<JobCardResponse> page = service.search(STUDENT_ID, NO_FILTERS, 5);

        assertTrue(page.getContent().isEmpty());
        assertEquals(2, page.getTotalElements());
        assertEquals(5, page.getNumber());
    }

    @Test
    void noResultsGivesAnEmptyPage() {
        repositoryReturns(List.of());

        Page<JobCardResponse> page = service.search(STUDENT_ID, NO_FILTERS, 0);

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());
    }

    @Test
    void aNegativePageIsRejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.search(STUDENT_ID, NO_FILTERS, -1));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
        verify(postings, never()).findAll(ArgumentMatchers.<Specification<Posting>>any(), any(Sort.class));
    }

    // --- fits my time ---

    @Test
    void fitsMyTimeKeepsPostingsCoveredByAtLeastHalf() {
        Posting sameSlot = posting(1, slot(14, 18));
        Posting shifted = posting(2, slot(15, 19));
        Posting otherDay = posting(3, new TimeSlot(WED + 1, 14 * 60, 18 * 60));
        repositoryReturns(List.of(sameSlot, shifted, otherDay));

        studentIsFreeAt(slot(14, 18)); // 100%, 75%, 0%
        assertEquals(List.of(1L, 2L), ids(service.search(STUDENT_ID, fitsMyTime(), 0)));

        studentIsFreeAt(slot(14, 16)); // 50% (exactly half is kept), 25%, 0%
        assertEquals(List.of(1L), ids(service.search(STUDENT_ID, fitsMyTime(), 0)));
    }

    @Test
    void fitsMyTimeJustBelowHalfIsDropped() {
        // 4.5 h job, free 2 h: 44%
        repositoryReturns(List.of(posting(1, new TimeSlot(WED, 14 * 60, 18 * 60 + 30))));
        studentIsFreeAt(slot(14, 16));

        assertTrue(service.search(STUDENT_ID, fitsMyTime(), 0).getContent().isEmpty());
    }

    @Test
    void fitsMyTimeWithoutFreeSlotsFindsNothing() {
        repositoryReturns(List.of(posting(1, slot(14, 18))));
        studentIsFreeAt();

        Page<JobCardResponse> page = service.search(STUDENT_ID, fitsMyTime(), 0);

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void fitsMyTimeCountsPagesAfterFiltering() {
        List<Posting> found = new ArrayList<>();
        for (long id = 1; id <= 30; id++) {
            // odd postings fit, even postings do not
            found.add(id % 2 == 1 ? posting(id, slot(14, 18)) : posting(id, slot(8, 12)));
        }
        repositoryReturns(found);
        studentIsFreeAt(slot(14, 18));

        Page<JobCardResponse> page = service.search(STUDENT_ID, fitsMyTime(), 0);

        assertEquals(15, page.getTotalElements());
        assertEquals(15, page.getContent().size());
        assertEquals(1, page.getTotalPages());
    }

    private static Filters fitsMyTime() {
        return new Filters(null, null, null, null, true);
    }

    // --- the SQL side: specification ---

    @Test
    void onlyOpenPostingsAreSearched() {
        Mocks mocks = runSpecification(NO_FILTERS);

        verify(mocks.cb).equal(any(), eq(PostingStatus.OPEN));
        verify(mocks.cb, never()).like(any(), ArgumentMatchers.<String>any(), ArgumentMatchers.anyChar());
    }

    @Test
    void blankKeywordAddsNoCondition() {
        Mocks mocks = runSpecification(new Filters("   ", List.of(), List.of(), null, false));

        verify(mocks.cb, never()).like(any(), ArgumentMatchers.<String>any(), ArgumentMatchers.anyChar());
        verify(mocks.cb, never()).or(any(jakarta.persistence.criteria.Predicate[].class));
        verify(mocks.root, never()).get("area");
        verify(mocks.root, never()).get("jobType");
        verify(mocks.root, never()).get("hourlyRate");
    }

    @Test
    void keywordIsTrimmedLowercasedAndMatchesTitleDescriptionAndTag() {
        Mocks mocks = runSpecification(new Filters("  Barista ", null, null, null, false));

        // title, description and the tag name all use the same pattern
        verify(mocks.cb, times(3)).like(any(), eq("%barista%"), eq('\\'));
        verify(mocks.cb).exists(any());
    }

    @Test
    void keywordWildcardsAreTreatedAsPlainCharacters() {
        Mocks mocks = runSpecification(new Filters("50%_off\\", null, null, null, false));

        verify(mocks.cb, times(3)).like(any(), eq("%50\\%\\_off\\\\%"), eq('\\'));
    }

    @Test
    void escapeLikeEscapesBackslashPercentAndUnderscore() {
        assertEquals("a\\%b\\_c\\\\d", PostingSearchService.escapeLike("a%b_c\\d"));
        assertEquals("plain", PostingSearchService.escapeLike("plain"));
    }

    @Test
    void areaTypeAndMinRateFiltersAreAddedWhenGiven() {
        List<Area> areas = List.of(Area.ATHLONE, Area.REMOTE);
        List<JobType> types = List.of(JobType.PART_TIME);
        BigDecimal minRate = new BigDecimal("12.00");

        Mocks mocks = runSpecification(new Filters(null, areas, types, minRate, false));

        verify(mocks.root.get("area")).in(areas);
        verify(mocks.root.get("jobType")).in(types);
        verify(mocks.cb).greaterThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), eq(minRate));
    }

    @Test
    void missingEmployerProfileLeavesTheNameEmpty() {
        repositoryReturns(List.of(posting(1)));
        when(employers.findAllById(any())).thenReturn(List.of());

        assertNull(service.search(STUDENT_ID, NO_FILTERS, 0).getContent().get(0).getEmployerName());
    }
}

