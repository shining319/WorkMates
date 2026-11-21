package com.workmates.common.timeslot;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * One weekly time slot: a day plus a start and end minute. Used for a student's free time and a posting's
 * work time. These are wall-clock times, so no time zone is involved.
 *
 * <p>Day is 1 (Monday) to 7 (Sunday). Minutes are counted from midnight, 0 to 1440, in 30-minute steps.
 * This class holds the values only; {@link TimeSlotRules} checks them.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA
public class TimeSlot {

    @SuppressWarnings("JpaDataSourceORMInspection")
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @SuppressWarnings("JpaDataSourceORMInspection")
    @Column(name = "start_minute", nullable = false)
    private int startMinute;

    @SuppressWarnings("JpaDataSourceORMInspection")
    @Column(name = "end_minute", nullable = false)
    private int endMinute;

    /** Minutes as {@code HH:mm}. 1440 gives {@code 24:00}. */
    public static String formatMinute(int minute) {
        return String.format("%02d:%02d", minute / 60, minute % 60);
    }

    /** Readable form used in error messages, for example {@code Monday 09:00-12:00}. */
    @Override
    public String toString() {
        String day = dayOfWeek >= 1 && dayOfWeek <= 7
                ? DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                : "Day " + dayOfWeek;
        return day + " " + formatMinute(startMinute) + "-" + formatMinute(endMinute);
    }
}
