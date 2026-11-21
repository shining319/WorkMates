package com.workmates.common.timeslot;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import java.util.Comparator;
import java.util.List;

/**
 * The rules every list of time slots must follow. Shared by student profiles and postings.
 * Every failure is a {@code TIME_SLOT_INVALID} whose message names the slots at fault.
 */
public final class TimeSlotRules {

    public static final int MAX_SLOTS = 21;
    private static final int STEP = 30;
    private static final int MINUTES_PER_DAY = 1440;

    private TimeSlotRules() {
    }

    /** Checks each slot, the list size, and that no two slots on the same day overlap. An empty list is fine. */
    public static void validate(List<TimeSlot> slots) {
        if (slots.size() > MAX_SLOTS) {
            throw invalid("At most " + MAX_SLOTS + " time slots are allowed");
        }
        for (TimeSlot slot : slots) {
            checkSlot(slot);
        }
        List<TimeSlot> sorted = slots.stream()
                .sorted(Comparator.comparingInt(TimeSlot::getDayOfWeek).thenComparingInt(TimeSlot::getStartMinute))
                .toList();
        // After sorting, if any two slots overlap then some neighbouring pair overlaps
        for (int i = 1; i < sorted.size(); i++) {
            TimeSlot previous = sorted.get(i - 1);
            TimeSlot current = sorted.get(i);
            if (previous.getDayOfWeek() == current.getDayOfWeek() && current.getStartMinute() < previous.getEndMinute()) {
                throw invalid("Time slots overlap: " + previous + " and " + current);
            }
        }
    }

    /** Total length of all slots in minutes. */
    public static int totalMinutes(List<TimeSlot> slots) {
        return slots.stream().mapToInt(s -> s.getEndMinute() - s.getStartMinute()).sum();
    }

    private static void checkSlot(TimeSlot slot) {
        if (slot.getDayOfWeek() < 1 || slot.getDayOfWeek() > 7) {
            throw invalid("Day must be Monday to Sunday: " + slot);
        }
        if (slot.getStartMinute() < 0 || slot.getEndMinute() > MINUTES_PER_DAY) {
            throw invalid("Times must be within one day: " + slot);
        }
        if (slot.getStartMinute() >= slot.getEndMinute()) {
            throw invalid("Start must be before end: " + slot);
        }
        if (slot.getStartMinute() % STEP != 0 || slot.getEndMinute() % STEP != 0) {
            throw invalid("Times must be in 30-minute steps: " + slot);
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.TIME_SLOT_INVALID, message);
    }
}
