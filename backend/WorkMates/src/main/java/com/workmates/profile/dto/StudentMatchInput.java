package com.workmates.profile.dto;

import com.workmates.common.timeslot.TimeSlot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** What matching needs from a student profile: free slots, tag IDs and expected hourly rate (may be null). */
public class StudentMatchInput {
    private final List<TimeSlot> timeSlots;
    private final Set<Long> tagIds;
    private final List<String> tagNames;
    private final BigDecimal expectedHourlyRate;

    public StudentMatchInput(List<TimeSlot> timeSlots, Set<Long> tagIds, BigDecimal expectedHourlyRate) {
        this(timeSlots, tagIds, expectedHourlyRate, List.of());
    }

    public StudentMatchInput(List<TimeSlot> timeSlots, Set<Long> tagIds,
                             BigDecimal expectedHourlyRate, List<String> tagNames) {
        this.timeSlots = timeSlots == null ? List.of() : List.copyOf(timeSlots);
        this.tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
        this.expectedHourlyRate = expectedHourlyRate;
        this.tagNames = tagNames == null ? List.of() : List.copyOf(tagNames);
    }

    public List<TimeSlot> getTimeSlots() { return timeSlots; }
    public Set<Long> getTagIds() { return tagIds; }
    public List<String> getTagNames() { return tagNames; }
    public BigDecimal getExpectedHourlyRate() { return expectedHourlyRate; }
}
