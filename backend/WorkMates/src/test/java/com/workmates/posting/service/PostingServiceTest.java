package com.workmates.posting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.posting.dto.PostingRequest;
import com.workmates.posting.dto.PostingResponse;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.repository.TagRepository;
import com.workmates.profile.service.TagService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PostingServiceTest {
    private static final Long EMPLOYER_ID = 5L;
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final PostingRepository postings = mock(PostingRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private final PostingService service = new PostingService(postings, new TagService(tagRepository));

    @BeforeEach
    void setUp() {
        when(postings.save(any(Posting.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tagRepository.findByNameIn(any())).thenReturn(List.of());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static TimeSlotDto slot(DayOfWeek day, String start, String end) {
        TimeSlotDto dto = new TimeSlotDto();
        dto.setDay(day);
        dto.setStart(start);
        dto.setEnd(end);
        return dto;
    }

    /** A valid request: 4 hours of slots and 4 weekly hours. */
    private static PostingRequest request() {
        PostingRequest r = new PostingRequest();
        r.setTitle("  Barista  ");
        r.setJobType(JobType.PART_TIME);
        r.setHourlyRate(new BigDecimal("13.50"));
        r.setWeeklyHours(new BigDecimal("4"));
        r.setArea(Area.ATHLONE);
        r.setAddress("  12 Church Street ");
        r.setDescription("Make coffee.");
        r.setHeadcount(2);
        r.setTags(List.of("English", "Customer Service"));
        r.setTimeSlots(List.of(slot(DayOfWeek.WEDNESDAY, "14:00", "18:00")));
        return r;
    }

    private static Set<String> invalidFields(PostingRequest r) {
        return VALIDATOR.validate(r).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static List<String> names(int count) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add("tag" + i);
        }
        return names;
    }

    // ---------- create ----------

    @Test
    void createSavesAnOpenPostingForTheEmployer() {
        PostingResponse response = service.create(EMPLOYER_ID, request());

        ArgumentCaptor<Posting> saved = ArgumentCaptor.forClass(Posting.class);
        verify(postings).save(saved.capture());
        Posting posting = saved.getValue();
        assertEquals(EMPLOYER_ID, posting.getEmployerId());
        assertEquals(PostingStatus.OPEN, posting.getStatus());
        assertNull(posting.getCloseReason());
        assertNull(posting.getClosedAt());
        assertEquals("Barista", posting.getTitle());
        assertEquals("12 Church Street", posting.getAddress());
        assertEquals(JobType.PART_TIME, posting.getJobType());
        assertEquals(Area.ATHLONE, posting.getArea());
        assertEquals(new BigDecimal("13.50"), posting.getHourlyRate());
        assertEquals(2, posting.getHeadcount());
        assertEquals(1, posting.getTimeSlots().size());
        assertEquals(PostingStatus.OPEN, response.getStatus());
        assertEquals("Barista", response.getTitle());
    }

    @Test
    void tagsAreNormalisedAndDuplicatesDropped() {
        PostingRequest r = request();
        r.setTags(List.of("Java", "JAVA", " java ", "English"));

        PostingResponse response = service.create(EMPLOYER_ID, r);

        assertEquals(List.of("english", "java"), response.getTags().stream().map(t -> t.getName()).toList());
    }

    @Test
    void existingTagsAreReusedAndNewOnesCreated() {
        Tag english = new Tag("english");
        when(tagRepository.findByNameIn(any())).thenReturn(List.of(english));
        PostingRequest r = request();
        r.setTags(List.of("English", "Barista"));

        service.create(EMPLOYER_ID, r);

        ArgumentCaptor<Tag> created = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(created.capture());
        assertEquals("barista", created.getValue().getName());
    }

    @Test
    void thereIsNoUpperLimitOnTags() {
        PostingRequest r = request();
        r.setTags(names(30));

        PostingResponse response = service.create(EMPLOYER_ID, r);

        assertEquals(30, response.getTags().size());
    }

    @Test
    void noTagsIsRejectedAndNothingIsSaved() {
        PostingRequest r = request();
        r.setTags(List.of());

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(EMPLOYER_ID, r));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
        verify(postings, never()).save(any());
    }

    @Test
    void blankTagIsRejected() {
        PostingRequest r = request();
        r.setTags(List.of("english", "  "));

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(EMPLOYER_ID, r));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
    }

    @Test
    void noTimeSlotsIsRejected() {
        PostingRequest r = request();
        r.setTimeSlots(List.of());

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(EMPLOYER_ID, r));

        assertEquals(ErrorCode.TIME_SLOT_INVALID, e.getCode());
        verify(postings, never()).save(any());
    }

    @Test
    void weeklyHoursAboveTheSlotsIsRejectedAndNoTagIsCreated() {
        PostingRequest r = request();
        r.setWeeklyHours(new BigDecimal("4.5"));
        r.setTags(List.of("brand new"));

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(EMPLOYER_ID, r));

        assertEquals(ErrorCode.WEEKLY_HOURS_EXCEED_SLOTS, e.getCode());
        verify(tagRepository, never()).save(any(Tag.class));
        verify(postings, never()).save(any());
    }

    @Test
    void weeklyHoursThatIsNotAMultipleOfHalfIsRejected() {
        PostingRequest r = request();
        r.setWeeklyHours(new BigDecimal("3.3"));

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(EMPLOYER_ID, r));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
    }

    @Test
    void blankDescriptionIsStoredAsNull() {
        PostingRequest r = request();
        r.setDescription("   ");

        PostingResponse response = service.create(EMPLOYER_ID, r);

        assertNull(response.getDescription());
    }

    @Test
    void responseListsSlotsByDayThenStart() {
        PostingRequest r = request();
        r.setWeeklyHours(new BigDecimal("7"));
        r.setTimeSlots(List.of(slot(DayOfWeek.FRIDAY, "10:00", "13:00"), slot(DayOfWeek.TUESDAY, "08:00", "12:00")));

        PostingResponse response = service.create(EMPLOYER_ID, r);

        assertEquals(DayOfWeek.TUESDAY, response.getTimeSlots().get(0).getDay());
        assertEquals(DayOfWeek.FRIDAY, response.getTimeSlots().get(1).getDay());
    }

    // ---------- request field rules ----------

    @Test
    void validRequestHasNoViolations() {
        assertEquals(Set.of(), invalidFields(request()));
    }

    @Test
    void titleIsRequiredAndAtMost100() {
        PostingRequest r = request();
        r.setTitle("   ");
        assertEquals(Set.of("title"), invalidFields(r));

        r.setTitle("a".repeat(100));
        assertEquals(Set.of(), invalidFields(r));
        r.setTitle("a".repeat(101));
        assertEquals(Set.of("title"), invalidFields(r));
    }

    @Test
    void jobTypeAndAreaAreRequired() {
        PostingRequest r = request();
        r.setJobType(null);
        r.setArea(null);

        assertEquals(Set.of("jobType", "area"), invalidFields(r));
    }

    @Test
    void hourlyRateIsAboveZeroWithAtMostTwoDecimals() {
        PostingRequest r = request();
        r.setHourlyRate(new BigDecimal("0"));
        assertEquals(Set.of("hourlyRate"), invalidFields(r));

        r.setHourlyRate(new BigDecimal("0.01"));
        assertEquals(Set.of(), invalidFields(r));
        r.setHourlyRate(new BigDecimal("13.505"));
        assertEquals(Set.of("hourlyRate"), invalidFields(r));
        r.setHourlyRate(null);
        assertEquals(Set.of("hourlyRate"), invalidFields(r));
    }

    @Test
    void weeklyHoursIsAtLeastHalfWithOneDecimal() {
        PostingRequest r = request();
        r.setWeeklyHours(new BigDecimal("0"));
        assertEquals(Set.of("weeklyHours"), invalidFields(r));

        r.setWeeklyHours(new BigDecimal("0.5"));
        assertEquals(Set.of(), invalidFields(r));
        r.setWeeklyHours(new BigDecimal("7.25"));
        assertEquals(Set.of("weeklyHours"), invalidFields(r));
        r.setWeeklyHours(null);
        assertEquals(Set.of("weeklyHours"), invalidFields(r));
    }

    @Test
    void addressIsRequiredAndAtMost200() {
        PostingRequest r = request();
        r.setAddress("");
        assertEquals(Set.of("address"), invalidFields(r));

        r.setAddress("a".repeat(200));
        assertEquals(Set.of(), invalidFields(r));
        r.setAddress("a".repeat(201));
        assertEquals(Set.of("address"), invalidFields(r));
    }

    @Test
    void descriptionIsOptionalAndAtMost2000() {
        PostingRequest r = request();
        r.setDescription(null);
        assertEquals(Set.of(), invalidFields(r));

        r.setDescription("a".repeat(2000));
        assertEquals(Set.of(), invalidFields(r));
        r.setDescription("a".repeat(2001));
        assertEquals(Set.of("description"), invalidFields(r));
    }

    @Test
    void headcountIsAWholeNumberFromOne() {
        PostingRequest r = request();
        r.setHeadcount(0);
        assertEquals(Set.of("headcount"), invalidFields(r));

        r.setHeadcount(1);
        assertEquals(Set.of(), invalidFields(r));
        r.setHeadcount(null);
        assertEquals(Set.of("headcount"), invalidFields(r));
    }

    @Test
    void tagsAndTimeSlotsMustBePresent() {
        PostingRequest r = request();
        r.setTags(null);
        r.setTimeSlots(null);

        assertEquals(Set.of("tags", "timeSlots"), invalidFields(r));
    }
}
