package com.workmates.posting.dto;

import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.matching.MatchScoreCalculator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** Student-visible explanation of why a posting matched (US 3.4). */
@Schema(description = "Student match breakdown; coverage uses explicit text labels, not just colours")
public record MatchBreakdownResponse(
        int overallPercent,
        int timePercent,
        int skillPercent,
        int payPercent,
        int matchedSkills,
        int requiredSkills,
        List<String> matchedTags,
        List<String> missingTags,
        BigDecimal jobHourlyRate,
        BigDecimal expectedHourlyRate,
        List<SlotCoverageResponse> jobSlotCoverage) {

    public record TimeRangeResponse(String start, String end, String coverage) {
        static TimeRangeResponse from(MatchScoreCalculator.MinuteRange range, String label) {
            return new TimeRangeResponse(TimeSlot.formatMinute(range.startMinute()),
                    TimeSlot.formatMinute(range.endMinute()), label);
        }
    }

    public record SlotCoverageResponse(TimeSlotDto jobSlot, List<TimeRangeResponse> covered,
                                       List<TimeRangeResponse> uncovered) {
        static SlotCoverageResponse from(MatchScoreCalculator.SlotCoverage slot) {
            return new SlotCoverageResponse(TimeSlotDto.from(slot.slot()),
                    slot.covered().stream().map(r -> TimeRangeResponse.from(r, "COVERED")).toList(),
                    slot.uncovered().stream().map(r -> TimeRangeResponse.from(r, "UNCOVERED")).toList());
        }
    }

    public static MatchBreakdownResponse from(MatchScoreCalculator.MatchScore match) {
        return new MatchBreakdownResponse(match.matchPercent(),
                (int) Math.round(match.timeScore() * 100),
                (int) Math.round(match.skillScore() * 100),
                (int) Math.round(match.payScore() * 100),
                match.matchedSkills(), match.requiredSkills(),
                match.matchedTags(), match.missingTags(),
                match.jobHourlyRate(), match.expectedHourlyRate(),
                match.time().slots().stream().map(SlotCoverageResponse::from).toList());
    }
}
