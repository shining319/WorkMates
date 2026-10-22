package com.workmates.common.timeslot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

class TimeSlotDtoTest {

    private static TimeSlotDto dto(DayOfWeek day, String start, String end) {
        TimeSlotDto dto = new TimeSlotDto();
        dto.setDay(day);
        dto.setStart(start);
        dto.setEnd(end);
        return dto;
    }

    @Test
    void convertsToMinutesAndDayNumber() {
        TimeSlot slot = dto(DayOfWeek.WEDNESDAY, "14:00", "18:30").toSlot();
        assertEquals(3, slot.getDayOfWeek());
        assertEquals(840, slot.getStartMinute());
        assertEquals(1110, slot.getEndMinute());
    }

    @Test
    void sundayIsSeven() {
        assertEquals(7, dto(DayOfWeek.SUNDAY, "09:00", "10:00").toSlot().getDayOfWeek());
    }

    @Test
    void twentyFourHundredMeansEndOfDay() {
        assertEquals(1440, dto(DayOfWeek.MONDAY, "22:00", "24:00").toSlot().getEndMinute());
    }

    @Test
    void convertsBackToJsonForm() {
        TimeSlotDto result = TimeSlotDto.from(new TimeSlot(5, 540, 1440));
        assertEquals(DayOfWeek.FRIDAY, result.getDay());
        assertEquals("09:00", result.getStart());
        assertEquals("24:00", result.getEnd());
    }

    @Test
    void badTimeTextIsTimeSlotInvalid() {
        for (String bad : new String[] {"9:00", "09:60", "25:00", "abc", "09-00"}) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> dto(DayOfWeek.MONDAY, bad, "10:00").toSlot(), bad);
            assertEquals(ErrorCode.TIME_SLOT_INVALID, e.getCode());
        }
    }
}
