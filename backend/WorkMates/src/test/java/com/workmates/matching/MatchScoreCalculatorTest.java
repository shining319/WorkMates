package com.workmates.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.workmates.common.timeslot.TimeSlot;
import com.workmates.matching.MatchScoreCalculator.MinuteRange;
import com.workmates.matching.MatchScoreCalculator.SlotCoverage;
import com.workmates.matching.MatchScoreCalculator.TimeMatch;
import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchScoreCalculatorTest {
    private static final int WED = DayOfWeek.WEDNESDAY.getValue();
    private static final int THU = DayOfWeek.THURSDAY.getValue();

    private static TimeSlot slot(int day, String start, String end) {
        return new TimeSlot(day, minutes(start), minutes(end));
    }

    private static int minutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static MinuteRange range(String start, String end) {
        return new MinuteRange(minutes(start), minutes(end));
    }

    @Test
    void partlyCoveredSlotGivesThePrdExample() {
        // PRD US 3.2: job Wed 14:00-18:00, free Wed 15:00-18:00 -> 75%
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(WED, "15:00", "18:00")), List.of(slot(WED, "14:00", "18:00")));

        assertEquals(180, match.coveredMinutes());
        assertEquals(240, match.totalMinutes());
        assertEquals(0.75, match.score());
        SlotCoverage coverage = match.slots().get(0);
        assertEquals(List.of(range("15:00", "18:00")), coverage.covered());
        assertEquals(List.of(range("14:00", "15:00")), coverage.uncovered());
    }

    @Test
    void severalFreeSlotsCoverOneJobSlotAndLeaveGaps() {
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(WED, "15:00", "16:00"), slot(WED, "10:00", "12:00"), slot(WED, "17:00", "20:00")),
                List.of(slot(WED, "11:00", "18:00")));

        assertEquals(List.of(range("11:00", "12:00"), range("15:00", "16:00"), range("17:00", "18:00")),
                match.slots().get(0).covered());
        assertEquals(List.of(range("12:00", "15:00"), range("16:00", "17:00")), match.slots().get(0).uncovered());
        assertEquals(180, match.coveredMinutes());
        assertEquals(420, match.totalMinutes());
    }

    @Test
    void minutesAreSummedOverAllJobSlots() {
        // Wed 4 h fully covered, Thu 4 h not covered: 240 / 480
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(WED, "09:00", "17:00")),
                List.of(slot(WED, "10:00", "14:00"), slot(THU, "10:00", "14:00")));

        assertEquals(0.5, match.score());
        assertEquals(List.of(range("10:00", "14:00")), match.slots().get(0).covered());
        assertEquals(List.of(), match.slots().get(0).uncovered());
        assertEquals(List.of(), match.slots().get(1).covered());
        assertEquals(List.of(range("10:00", "14:00")), match.slots().get(1).uncovered());
    }

    @Test
    void freeSlotsOnAnotherDayDoNotCount() {
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(THU, "00:00", "24:00")), List.of(slot(WED, "14:00", "18:00")));

        assertEquals(0, match.coveredMinutes());
        assertEquals(0.0, match.score());
    }

    @Test
    void noFreeSlotsScoresZero() {
        TimeMatch match = MatchScoreCalculator.timeMatch(List.of(), List.of(slot(WED, "14:00", "18:00")));

        assertEquals(0.0, match.score());
        assertEquals(List.of(range("14:00", "18:00")), match.slots().get(0).uncovered());
    }

    @Test
    void aPostingWithoutSlotsScoresZero() {
        TimeMatch match = MatchScoreCalculator.timeMatch(List.of(slot(WED, "14:00", "18:00")), List.of());

        assertEquals(0.0, match.score());
        assertEquals(List.of(), match.slots());
    }

    @Test
    void exactlyHalfCoveredScoresExactlyOneHalf() {
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(WED, "14:00", "16:00")), List.of(slot(WED, "14:00", "18:00")));

        assertEquals(0.5, match.score());
    }

    @Test
    void touchingSlotsDoNotOverlap() {
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(WED, "10:00", "14:00")), List.of(slot(WED, "14:00", "18:00")));

        assertEquals(0, match.coveredMinutes());
    }

    @Test
    void aFreeSlotLargerThanTheJobSlotCoversItAll() {
        TimeMatch match = MatchScoreCalculator.timeMatch(
                List.of(slot(WED, "00:00", "24:00")), List.of(slot(WED, "14:00", "18:00")));

        assertEquals(1.0, match.score());
        assertEquals(List.of(range("14:00", "18:00")), match.slots().get(0).covered());
        assertEquals(List.of(), match.slots().get(0).uncovered());
    }
}
