package com.workmates.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workmates.account.entity.User;
import com.workmates.account.service.AccountService;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.dto.StudentProfileRequest;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.entity.StudentProfile;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.repository.StudentProfileRepository;
import com.workmates.profile.repository.TagRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StudentProfileServiceTest {
    private static final Long ID = 7L;

    private final StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private final AccountService accounts = mock(AccountService.class);
    private final StudentProfileService service =
            new StudentProfileService(profiles, new TagService(tagRepository), accounts);

    private StudentProfile profile;

    @BeforeEach
    void setUp() {
        profile = new StudentProfile();
        profile.setUserId(ID);
        profile.setMatchAlertsEnabled(true);
        when(profiles.findById(ID)).thenReturn(Optional.of(profile));
        User user = new User();
        user.setName("Alex Murphy");
        when(accounts.getUser(ID)).thenReturn(user);
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

    private static StudentProfileRequest request() {
        StudentProfileRequest r = new StudentProfileRequest();
        r.setName("  Alex M  ");
        r.setExpectedHourlyRate(new BigDecimal("13.50"));
        r.setTags(List.of("Java", "English"));
        r.setTimeSlots(List.of(slot(DayOfWeek.MONDAY, "09:00", "12:00")));
        return r;
    }

    @Test
    void newProfileIsEmptyAndListsEverythingMissing() {
        StudentProfileResponse response = service.get(ID);

        assertFalse(response.isComplete());
        assertEquals(List.of("tags", "expectedHourlyRate", "timeSlots"), response.getMissing());
        assertEquals("Alex Murphy", response.getName());
        assertTrue(response.getTags().isEmpty());
        assertFalse(service.isComplete(ID));
    }

    @Test
    void unknownStudentIsNotFound() {
        when(profiles.findById(99L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.get(99L));
        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
    }

    @Test
    void savingFullProfileMakesItComplete() {
        StudentProfileRequest r = request();
        r.setPhone("  +353 87 1  ");
        r.setMajor("Software");
        r.setYearOfStudy("Year 2");

        StudentProfileResponse response = service.update(ID, r);

        assertTrue(response.isComplete());
        assertTrue(response.getMissing().isEmpty());
        assertEquals("+353 87 1", profile.getPhone());
        assertEquals("Software", profile.getMajor());
        assertEquals("Year 2", profile.getYearOfStudy());
        assertEquals(new BigDecimal("13.50"), profile.getExpectedHourlyRate());
        assertEquals(List.of("english", "java"), response.getTags().stream().map(t -> t.getName()).toList());
        assertEquals(1, profile.getTimeSlots().size());
        assertTrue(service.isComplete(ID));
    }

    @Test
    void nameGoesToTheAccount() {
        service.update(ID, request());

        verify(accounts).changeName(ID, "Alex M");
    }

    @Test
    void blankOptionalTextIsStoredAsNull() {
        StudentProfileRequest r = request();
        r.setPhone("   ");
        r.setMajor("");

        service.update(ID, r);

        assertNull(profile.getPhone());
        assertNull(profile.getMajor());
        assertNull(profile.getYearOfStudy());
    }

    @Test
    void missingRateIsListedAsMissing() {
        StudentProfileRequest r = request();
        r.setExpectedHourlyRate(null);

        StudentProfileResponse response = service.update(ID, r);

        assertFalse(response.isComplete());
        assertEquals(List.of("expectedHourlyRate"), response.getMissing());
    }

    @Test
    void duplicateTagsAreIgnoredIgnoringCase() {
        StudentProfileRequest r = request();
        r.setTags(List.of("Java", "JAVA", " java "));

        service.update(ID, r);

        assertEquals(1, profile.getTags().size());
    }

    private static List<String> tags(int count) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add("tag" + i);
        }
        return names;
    }

    @Test
    void twentyTagsAreAllowed() {
        StudentProfileRequest r = request();
        r.setTags(tags(20));

        service.update(ID, r);

        assertEquals(20, profile.getTags().size());
    }

    @Test
    void twentyOneTagsAreRejectedAndNothingIsChanged() {
        StudentProfileRequest r = request();
        r.setTags(tags(21));

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(ID, r));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
        verify(accounts, never()).changeName(any(), any());
        assertTrue(profile.getTags().isEmpty());
    }

    @Test
    void twentyOneTagsWithDuplicatesThatShrinkToTwentyAreAllowed() {
        StudentProfileRequest r = request();
        List<String> names = tags(20);
        names.add("TAG0");
        r.setTags(names);

        service.update(ID, r);

        assertEquals(20, profile.getTags().size());
    }

    @Test
    void blankTagIsRejected() {
        StudentProfileRequest r = request();
        r.setTags(List.of("java", "  "));

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(ID, r));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
    }

    @Test
    void overlappingSlotsAreRejectedAndNothingIsChanged() {
        StudentProfileRequest r = request();
        r.setTimeSlots(List.of(slot(DayOfWeek.MONDAY, "09:00", "12:00"), slot(DayOfWeek.MONDAY, "11:00", "13:00")));

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(ID, r));

        assertEquals(ErrorCode.TIME_SLOT_INVALID, e.getCode());
        verify(accounts, never()).changeName(any(), any());
        assertTrue(profile.getTimeSlots().isEmpty());
    }

    @Test
    void savingReplacesTheWholeSetOfSlotsAndTags() {
        service.update(ID, request());
        StudentProfileRequest second = request();
        second.setTags(List.of("python"));
        second.setTimeSlots(List.of(slot(DayOfWeek.FRIDAY, "10:00", "12:00"), slot(DayOfWeek.TUESDAY, "08:00", "09:00")));

        StudentProfileResponse response = service.update(ID, second);

        assertEquals(List.of("python"), response.getTags().stream().map(t -> t.getName()).toList());
        assertEquals(2, profile.getTimeSlots().size());
        // The response lists slots by day, then start
        assertEquals(DayOfWeek.TUESDAY, response.getTimeSlots().get(0).getDay());
        assertEquals(DayOfWeek.FRIDAY, response.getTimeSlots().get(1).getDay());
    }

    @Test
    void emptyTagsAndSlotsClearTheProfile() {
        service.update(ID, request());
        StudentProfileRequest empty = request();
        empty.setTags(List.of());
        empty.setTimeSlots(List.of());

        StudentProfileResponse response = service.update(ID, empty);

        assertFalse(response.isComplete());
        assertEquals(List.of("tags", "timeSlots"), response.getMissing());
    }

    @Test
    void matchInputHasSlotsTagIdsAndRate() {
        Tag java = new Tag("java");
        ReflectionTestUtils.setField(java, "id", 3L);
        profile.getTags().add(java);
        profile.getTimeSlots().add(new TimeSlot(1, 540, 720));
        profile.setExpectedHourlyRate(new BigDecimal("12.00"));

        StudentMatchInput input = service.matchInput(ID);

        assertEquals(Set.of(3L), input.getTagIds());
        assertEquals(List.of(new TimeSlot(1, 540, 720)), input.getTimeSlots());
        assertEquals(new BigDecimal("12.00"), input.getExpectedHourlyRate());
    }
}
