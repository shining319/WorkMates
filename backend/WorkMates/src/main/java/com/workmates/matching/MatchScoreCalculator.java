package com.workmates.matching;

import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.entity.Posting;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.entity.Tag;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure functions that compare a student with a posting. No database access.
 * US 3.2 time coverage is retained and shared with US 3.3 ranking and US 3.4 details.
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
    /** Scores in the inclusive range 0..1. The exact overall value is used for sorting. */
    public record MatchScore(TimeMatch time, double timeScore, double skillScore, double payScore,
                             double overallScore, int matchPercent,
                             int matchedSkills, int requiredSkills,
                             List<String> matchedTags, List<String> missingTags,
                             BigDecimal jobHourlyRate, BigDecimal expectedHourlyRate) {
    }

    /** Calculates time, case-insensitive skills and pay, giving zero to any missing profile part. */
    public static MatchScore calculate(StudentMatchInput student, Posting job) {
        List<TimeSlot> free = student == null || student.getTimeSlots() == null
                ? List.of() : student.getTimeSlots();
        List<TimeSlot> required = job.getTimeSlots() == null ? List.of() : job.getTimeSlots();
        TimeMatch time = timeMatch(free, required);

        Set<String> studentTags = new HashSet<>();
        if (student != null && student.getTagNames() != null) {
            for (String tag : student.getTagNames()) {
                if (tag != null && !tag.isBlank()) {
                    studentTags.add(tag.trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        Set<String> seen = new HashSet<>();
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        if (job.getTags() != null) {
            for (Tag tag : job.getTags()) {
                if (tag == null || tag.getName() == null || tag.getName().isBlank()) continue;
                String normalized = tag.getName().trim().toLowerCase(Locale.ROOT);
                if (!seen.add(normalized)) continue;
                if (studentTags.contains(normalized)) matched.add(tag.getName());
                else missing.add(tag.getName());
            }
        }
        matched.sort(String.CASE_INSENSITIVE_ORDER);
        missing.sort(String.CASE_INSENSITIVE_ORDER);
        int requiredCount = matched.size() + missing.size();
        double skillScore = requiredCount == 0 || studentTags.isEmpty()
                ? 0.0 : (double) matched.size() / requiredCount;

        BigDecimal rate = job.getHourlyRate();
        BigDecimal expected = student == null ? null : student.getExpectedHourlyRate();
        double payScore = 0.0;
        if (expected != null && expected.signum() > 0 && rate != null && rate.signum() >= 0) {
            payScore = rate.compareTo(expected) >= 0 ? 1.0
                    : rate.divide(expected, MathContext.DECIMAL128).doubleValue();
        }
        double overall = (time.score() + skillScore + payScore) / 3.0;
        return new MatchScore(time, time.score(), skillScore, payScore, overall,
                (int) Math.round(overall * 100.0), matched.size(), requiredCount,
                List.copyOf(matched), List.copyOf(missing), rate, expected);
    }

}
