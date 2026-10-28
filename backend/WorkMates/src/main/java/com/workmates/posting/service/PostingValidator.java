package com.workmates.posting.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotRules;
import java.math.BigDecimal;
import java.util.List;

/** The posting rules that Bean Validation cannot express. Used when a posting is created and when it is edited. */
final class PostingValidator {
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

    private PostingValidator() {
    }

    /**
     * Checks the time slots (at least one, and the shared slot rules) and the weekly hours (a multiple of 0.5,
     * not more than the total length of the slots).
     */
    static void validate(BigDecimal weeklyHours, List<TimeSlot> slots) {
        if (slots.isEmpty()) {
            throw new BusinessException(ErrorCode.TIME_SLOT_INVALID, "At least one time slot is required");
        }
        TimeSlotRules.validate(slots);
        if (weeklyHours.remainder(HALF).signum() != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Weekly hours must be a multiple of 0.5");
        }
        int totalMinutes = TimeSlotRules.totalMinutes(slots);
        if (weeklyHours.multiply(SIXTY).compareTo(BigDecimal.valueOf(totalMinutes)) > 0) {
            String totalHours = BigDecimal.valueOf(totalMinutes).divide(SIXTY).stripTrailingZeros().toPlainString();
            throw new BusinessException(ErrorCode.WEEKLY_HOURS_EXCEED_SLOTS,
                    ErrorCode.WEEKLY_HOURS_EXCEED_SLOTS.getDefaultMessage() + " (" + totalHours + " h)");
        }
    }
}
