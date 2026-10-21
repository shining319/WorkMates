package com.workmates.profile.dto;

import com.workmates.common.timeslot.TimeSlot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** What matching needs from a student profile: free slots, tag IDs and expected hourly rate (may be null). */
public class StudentMatchInput {
    private final List<TimeSlot> timeSlots;
    private final Set<Long> tagIds;
    private final BigDecimal expectedHourlyRate;

    public StudentMatchInput(List<TimeSlot> timeSlots, Set<Long> tagIds, BigDecimal expectedHourlyRate) {
        this.timeSlots = timeSlots;
        this.tagIds = tagIds;
        this.expectedHourlyRate = expectedHourlyRate;
    }

    public List<TimeSlot> getTimeSlots() { return timeSlots; }
    public Set<Long> getTagIds() { return tagIds; }
    public BigDecimal getExpectedHourlyRate() { return expectedHourlyRate; }
}
