package com.workmates.common.timeslot;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A time slot as it appears in JSON: {@code {"day":"WEDNESDAY","start":"14:00","end":"18:00"}}. */
@Schema(description = "A weekly time slot in 30-minute steps")
public class TimeSlotDto {

    private static final Pattern HH_MM = Pattern.compile("^(\\d{2}):(\\d{2})$");

    @NotNull
    @Schema(description = "Day of the week", example = "WEDNESDAY", requiredMode = Schema.RequiredMode.REQUIRED)
    private DayOfWeek day;

    @NotBlank
    @Schema(description = "Start time, HH:mm", example = "14:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private String start;

    @NotBlank
    @Schema(description = "End time, HH:mm. 24:00 means midnight at the end of the day.", example = "18:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String end;

    public DayOfWeek getDay() { return day; }
    public void setDay(DayOfWeek day) { this.day = day; }
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    /** Converts to the entity form. Throws {@code TIME_SLOT_INVALID} when a time is not {@code HH:mm}. */
    public TimeSlot toSlot() {
        return new TimeSlot(day.getValue(), parseMinute(start), parseMinute(end));
    }

    public static TimeSlotDto from(TimeSlot slot) {
        TimeSlotDto dto = new TimeSlotDto();
        dto.setDay(DayOfWeek.of(slot.getDayOfWeek()));
        dto.setStart(TimeSlot.formatMinute(slot.getStartMinute()));
        dto.setEnd(TimeSlot.formatMinute(slot.getEndMinute()));
        return dto;
    }

    public static List<TimeSlot> toSlots(List<TimeSlotDto> dtos) {
        return dtos.stream().map(TimeSlotDto::toSlot).toList();
    }

    private static int parseMinute(String text) {
        Matcher m = HH_MM.matcher(text.trim());
        if (!m.matches()) {
            throw new BusinessException(ErrorCode.TIME_SLOT_INVALID, "Time must look like 14:30, got '" + text + "'");
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));
        if (minute > 59 || hour > 24) {
            throw new BusinessException(ErrorCode.TIME_SLOT_INVALID, "Time must look like 14:30, got '" + text + "'");
        }
        return hour * 60 + minute;
    }
}
