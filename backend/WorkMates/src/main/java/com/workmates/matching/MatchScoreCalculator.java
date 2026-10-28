package com.workmates.matching;

import com.workmates.common.timeslot.TimeSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure functions that compare a student with a posting. No database access.
 * For now it holds the time score only; the skill and pay scores come with US 3.3.
 */
public final class MatchScoreCalculator {

    private MatchScoreCalculator() {
    }

    /** A stretch of one day, in minutes from midnight. */
    public record MinuteRange(int startMinute, int endMinute) {
    }

    /** One slot of the posting, split into the parts the student's free time covers and the parts it does not. */
    public record SlotCoverage(TimeSlot slot, List<MinuteRange> covered, List<MinuteRange> uncovered) {
    }

    /** How much of the posting's weekly slot time the student's free slots cover. */
    public record TimeMatch(int coveredMinutes, int totalMinutes, List<SlotCoverage> slots) {
        /** Covered minutes divided by total job slot minutes, from 0 to 1. A posting with no slots scores 0. */
        public double score() {
            return totalMinutes == 0 ? 0 : (double) coveredMinutes / totalMinutes;
        }
    }

    /**
     * Time score: for every job slot, the overlap with the student's free slots on the same day,
     * {@code max(0, min(end) - max(start))}, summed and divided by the total job slot minutes.
     * The free slots must not overlap each other (the slot rules guarantee this), or minutes would count twice.
     */
    public static TimeMatch timeMatch(List<TimeSlot> freeSlots, List<TimeSlot> jobSlots) {
        List<SlotCoverage> coverages = new ArrayList<>();
        int covered = 0;
        int total = 0;
        for (TimeSlot job : jobSlots) {
            SlotCoverage coverage = coverageOf(job, freeSlots);
            coverages.add(coverage);
            covered += coverage.covered().stream().mapToInt(r -> r.endMinute() - r.startMinute()).sum();
            total += job.getEndMinute() - job.getStartMinute();
        }
        return new TimeMatch(covered, total, coverages);
    }

    private static SlotCoverage coverageOf(TimeSlot job, List<TimeSlot> freeSlots) {
        List<MinuteRange> covered = new ArrayList<>();
        List<MinuteRange> uncovered = new ArrayList<>();
        List<TimeSlot> sameDay = freeSlots.stream()
                .filter(free -> free.getDayOfWeek() == job.getDayOfWeek())
                .sorted(Comparator.comparingInt(TimeSlot::getStartMinute))
                .toList();
        int cursor = job.getStartMinute();
        for (TimeSlot free : sameDay) {
            int start = Math.max(free.getStartMinute(), cursor);
            int end = Math.min(free.getEndMinute(), job.getEndMinute());
            if (start >= end) {
                continue;
            }
            if (start > cursor) {
                uncovered.add(new MinuteRange(cursor, start));
            }
            covered.add(new MinuteRange(start, end));
            cursor = end;
        }
        if (cursor < job.getEndMinute()) {
            uncovered.add(new MinuteRange(cursor, job.getEndMinute()));
        }
        return new SlotCoverage(job, covered, uncovered);
    }
}
