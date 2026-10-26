package com.workmates.matching;

import com.workmates.common.timeslot.TimeSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure weekly minute-overlap calculator for US 3.2. No database calls or rounding for the 50% check. */
public final class TimeCoverageCalculator {
    private TimeCoverageCalculator() { }

    public static boolean fits(List<TimeSlot> jobSlots, List<TimeSlot> studentSlots) {
        Coverage coverage = calculate(jobSlots, studentSlots);
        return coverage.jobMinutes() > 0 && 2L * coverage.coveredMinutes() >= coverage.jobMinutes();
    }

    public static Coverage calculate(List<TimeSlot> jobSlots, List<TimeSlot> studentSlots) {
        Map<Integer, List<Interval>> jobs = mergeByDay(jobSlots);
        Map<Integer, List<Interval>> free = mergeByDay(studentSlots);
        int jobMinutes = 0;
        int covered = 0;
        for (var day : jobs.entrySet()) {
            List<Interval> work = day.getValue();
            List<Interval> available = free.getOrDefault(day.getKey(), List.of());
            for (Interval interval : work) {
                jobMinutes += interval.end - interval.start;
            }
            int i = 0;
            int j = 0;
            while (i < work.size() && j < available.size()) {
                Interval w = work.get(i);
                Interval a = available.get(j);
                covered += Math.max(0, Math.min(w.end, a.end) - Math.max(w.start, a.start));
                if (w.end <= a.end) i++;
                else j++;
            }
        }
        return new Coverage(covered, jobMinutes);
    }

    /** Merging also guards against duplicated or overlapping legacy records; each minute counts at most once. */
    private static Map<Integer, List<Interval>> mergeByDay(List<TimeSlot> slots) {
        Map<Integer, List<Interval>> grouped = new HashMap<>();
        for (TimeSlot slot : slots) {
            if (slot.getEndMinute() <= slot.getStartMinute()) continue;
            grouped.computeIfAbsent(slot.getDayOfWeek(), ignored -> new ArrayList<>())
                    .add(new Interval(slot.getStartMinute(), slot.getEndMinute()));
        }
        for (var entry : grouped.entrySet()) {
            List<Interval> sorted = entry.getValue();
            sorted.sort(Comparator.comparingInt(Interval::start).thenComparingInt(Interval::end));
            List<Interval> merged = new ArrayList<>();
            for (Interval interval : sorted) {
                if (merged.isEmpty() || interval.start > merged.get(merged.size() - 1).end) {
                    merged.add(interval);
                } else {
                    Interval last = merged.remove(merged.size() - 1);
                    merged.add(new Interval(last.start, Math.max(last.end, interval.end)));
                }
            }
            entry.setValue(merged);
        }
        return grouped;
    }

    public record Coverage(int coveredMinutes, int jobMinutes) {
        public double percent() {
            return jobMinutes == 0 ? 0 : 100.0 * coveredMinutes / jobMinutes;
        }
    }

    private record Interval(int start, int end) { }
}
