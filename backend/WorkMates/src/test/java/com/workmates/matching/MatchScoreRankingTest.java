package com.workmates.matching;

import static org.junit.jupiter.api.Assertions.*;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.posting.entity.Posting;
import com.workmates.profile.dto.StudentMatchInput;
import com.workmates.profile.entity.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MatchScoreRankingTest {
    private static Posting job(String pay, String... skills) {
        Posting p = new Posting();
        p.setHourlyRate(new BigDecimal(pay));
        p.getTimeSlots().add(new TimeSlot(3, 840, 1080));
        for (String skill : skills) p.getTags().add(new Tag(skill));
        return p;
    }
    private static StudentMatchInput student(String expected, List<TimeSlot> slots, String... skills) {
        return new StudentMatchInput(slots, Set.of(), expected == null ? null : new BigDecimal(expected),
                List.of(skills));
    }

    @Test void allThreeScoresAreEquallyWeightedAndSkillsIgnoreCase() {
        var match = MatchScoreCalculator.calculate(student("14", List.of(new TimeSlot(3, 900, 1080)),
                "JAVA", " TeamWork "), job("15", "java", "teamwork", "Customer Service"));
        assertEquals(0.75, match.timeScore(), 1e-12);
        assertEquals(2.0 / 3, match.skillScore(), 1e-12);
        assertEquals(1.0, match.payScore(), 1e-12);
        assertEquals((0.75 + 2.0 / 3 + 1.0) / 3, match.overallScore(), 1e-12);
        assertEquals(81, match.matchPercent());
        assertEquals(List.of("Customer Service"), match.missingTags());
    }

    @Test void missingProfilePartsScoreZero() {
        var match = MatchScoreCalculator.calculate(student(null, List.of()), job("15", "Cashier"));
        assertEquals(0, match.overallScore());
        assertEquals(0, match.matchPercent());
    }

    @Test void payBelowExpectedIsRatio() {
        var match = MatchScoreCalculator.calculate(student("20", List.of()), job("15"));
        assertEquals(0.75, match.payScore(), 1e-12);
    }

    @Test void missingEntireInputIsZero() {
        assertEquals(0, MatchScoreCalculator.calculate(null, job("15", "Food")).overallScore());
    }
}
