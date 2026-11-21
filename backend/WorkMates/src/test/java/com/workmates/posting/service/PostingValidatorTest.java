package com.workmates.posting.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostingValidatorTest {
    // Wednesday 14:00-18:00 is 4 hours
    private static final TimeSlot WED_4H = new TimeSlot(3, 840, 1080);
    // Friday 10:00-13:30 is 3.5 hours
    private static final TimeSlot FRI_3H30 = new TimeSlot(5, 600, 810);

    private static BusinessException rejected(String weeklyHours, List<TimeSlot> slots) {
        return assertThrows(BusinessException.class,
                () -> PostingValidator.validate(new BigDecimal(weeklyHours), slots));
    }

    @Test
    void weeklyHoursEqualToTotalSlotLengthIsAllowed() {
        assertDoesNotThrow(() -> PostingValidator.validate(new BigDecimal("4"), List.of(WED_4H)));
        assertDoesNotThrow(() -> PostingValidator.validate(new BigDecimal("7.5"), List.of(WED_4H, FRI_3H30)));
    }

    @Test
    void weeklyHoursBelowTotalSlotLengthIsAllowed() {
        assertDoesNotThrow(() -> PostingValidator.validate(new BigDecimal("0.5"), List.of(WED_4H)));
    }

    @Test
    void weeklyHoursMoreThanTotalSlotLengthIsRejectedWithTheTotal() {
        BusinessException e = rejected("4.5", List.of(WED_4H));

        assertEquals(ErrorCode.WEEKLY_HOURS_EXCEED_SLOTS, e.getCode());
        assertEquals("Weekly hours cannot be more than the total length of the time slots (4 h)", e.getMessage());
    }

    @Test
    void totalIsSummedOverAllSlotsAndKeepsAHalfHour() {
        BusinessException e = rejected("8", List.of(WED_4H, FRI_3H30));

        assertEquals(ErrorCode.WEEKLY_HOURS_EXCEED_SLOTS, e.getCode());
        assertTrue(e.getMessage().endsWith("(7.5 h)"));
    }

    @Test
    void weeklyHoursMustBeAMultipleOfHalf() {
        BusinessException e = rejected("3.3", List.of(WED_4H));

        assertEquals(ErrorCode.VALIDATION_FAILED, e.getCode());
        assertDoesNotThrow(() -> PostingValidator.validate(new BigDecimal("3.0"), List.of(WED_4H)));
    }

    @Test
    void atLeastOneTimeSlotIsRequired() {
        BusinessException e = rejected("4", List.of());

        assertEquals(ErrorCode.TIME_SLOT_INVALID, e.getCode());
    }

    @Test
    void sharedSlotRulesApply() {
        List<TimeSlot> overlapping = List.of(new TimeSlot(3, 840, 1080), new TimeSlot(3, 960, 1200));

        assertEquals(ErrorCode.TIME_SLOT_INVALID, rejected("4", overlapping).getCode());
    }
}
