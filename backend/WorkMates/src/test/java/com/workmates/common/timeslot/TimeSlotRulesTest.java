package com.workmates.common.timeslot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimeSlotRulesTest {

    private static TimeSlot slot(int day, int start, int end) {
        return new TimeSlot(day, start, end);
    }

    private static BusinessException rejected(List<TimeSlot> slots) {
        BusinessException e = assertThrows(BusinessException.class, () -> TimeSlotRules.validate(slots));
        assertEquals(ErrorCode.TIME_SLOT_INVALID, e.getCode());
        return e;
    }

    @Test
    void emptyListIsValid() {
        assertDoesNotThrow(() -> TimeSlotRules.validate(List.of()));
    }

    @Test
    void acceptsWholeDayFromZeroTo1440() {
        assertDoesNotThrow(() -> TimeSlotRules.validate(List.of(slot(1, 0, 1440))));
    }

    @Test
    void rejectsEndAfter1440() {
        rejected(List.of(slot(1, 1410, 1470)));
    }

    @Test
    void rejectsNegativeStart() {
        rejected(List.of(slot(1, -30, 60)));
    }

    @Test
    void rejectsStartNotBeforeEnd() {
        rejected(List.of(slot(1, 600, 600)));
        rejected(List.of(slot(1, 660, 600)));
    }

    @Test
    void rejectsTimesOutsideThirtyMinuteSteps() {
        rejected(List.of(slot(1, 545, 600)));
        rejected(List.of(slot(1, 540, 610)));
    }

    @Test
    void rejectsDayOutsideOneToSeven() {
        rejected(List.of(slot(0, 540, 600)));
        rejected(List.of(slot(8, 540, 600)));
    }

    @Test
    void overlapMessageNamesBothSlots() {
        // Given in reverse order, to prove the check sorts first
        BusinessException e = rejected(List.of(slot(1, 660, 840), slot(1, 540, 720)));
        assertTrue(e.getMessage().contains("Monday 09:00-12:00"), e.getMessage());
        assertTrue(e.getMessage().contains("Monday 11:00-14:00"), e.getMessage());
    }

    @Test
    void rejectsSlotInsideAnotherSlot() {
        rejected(List.of(slot(2, 540, 900), slot(2, 600, 660), slot(2, 700, 720)));
    }

    @Test
    void rejectsIdenticalSlots() {
        rejected(List.of(slot(3, 540, 600), slot(3, 540, 600)));
    }

    @Test
    void touchingSlotsAreNotOverlapping() {
        assertDoesNotThrow(() -> TimeSlotRules.validate(List.of(slot(1, 540, 600), slot(1, 600, 660))));
    }

    @Test
    void sameTimesOnDifferentDaysAreNotOverlapping() {
        assertDoesNotThrow(() -> TimeSlotRules.validate(List.of(slot(1, 540, 600), slot(2, 540, 600))));
    }

    @Test
    void allowsTwentyOneSlotsButNotTwentyTwo() {
        List<TimeSlot> slots = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            slots.add(slot(i % 7 + 1, (i / 7) * 120, (i / 7) * 120 + 60));
        }
        assertDoesNotThrow(() -> TimeSlotRules.validate(slots));

        slots.add(slot(1, 600, 660));
        BusinessException e = rejected(slots);
        assertTrue(e.getMessage().contains("21"), e.getMessage());
    }

    @Test
    void totalMinutesAddsAllSlots() {
        assertEquals(240 + 60, TimeSlotRules.totalMinutes(List.of(slot(1, 540, 780), slot(2, 0, 60))));
        assertEquals(0, TimeSlotRules.totalMinutes(List.of()));
    }
}
