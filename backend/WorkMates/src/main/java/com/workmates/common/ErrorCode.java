package com.workmates.common;

import org.springframework.http.HttpStatus;

/**
 * Every business error the API can return. The name is sent to the frontend as {@code errorCode}.
 * Each code carries its HTTP status and a default message.
 *
 * <p>Keep this list in sync with the error table in the architecture doc (section 5.5) and with the
 * frontend's {@code ErrorCode} type. A message that needs values (for example the current count)
 * is built by the caller and passed to {@link BusinessException#BusinessException(ErrorCode, String)}.
 */
public enum ErrorCode {

    /** Any request field that fails a format check. The message is the first invalid field's message. */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Invalid input."),
    /** A student registers with an email outside @student.tus.ie. */
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "Student accounts need a @student.tus.ie email."),
    /** An employer registers with a @student.tus.ie email. */
    STUDENT_EMAIL_FOR_EMPLOYER(HttpStatus.BAD_REQUEST, "Student emails can only be used for student accounts."),
    /** The email is already registered, also when two people register it at the same moment. */
    EMAIL_TAKEN(HttpStatus.CONFLICT, "An account with this email already exists. Please log in."),
    /** Login failed. The same code is used for a wrong email and a wrong password. */
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"),
    /** The token is missing, invalid or expired. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Please log in again."),
    /** The caller's role is not allowed to use this endpoint. */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have access to this page"),
    /** The item does not exist, or it belongs to someone else. Both look the same on purpose. */
    NOT_FOUND(HttpStatus.NOT_FOUND, "This item is no longer available"),
    /** A time slot is out of range, overlaps another one, or there are too many slots. */
    TIME_SLOT_INVALID(HttpStatus.BAD_REQUEST, "The time slots are invalid."),
    /** Weekly hours are more than the total length of the posting's time slots. */
    WEEKLY_HOURS_EXCEED_SLOTS(HttpStatus.BAD_REQUEST,
            "Weekly hours cannot be more than the total length of the time slots"),
    /** A student applies before the profile is complete. */
    PROFILE_INCOMPLETE(HttpStatus.CONFLICT, "Complete your profile before applying"),
    /** A student applies to the same posting twice. */
    ALREADY_APPLIED(HttpStatus.CONFLICT, "You have already applied to this job"),
    /** A student applies to a closed posting. */
    POSTING_CLOSED(HttpStatus.CONFLICT, "This posting is closed"),
    /** An employer edits or closes a posting that is already closed. */
    POSTING_NOT_EDITABLE(HttpStatus.CONFLICT, "This posting is closed and can no longer be changed"),
    /** The new headcount is below the current Accepted + Completed count. */
    HEADCOUNT_BELOW_FILLED(HttpStatus.CONFLICT,
            "Headcount cannot be lower than the current Accepted + Completed count"),
    /** The application state machine does not allow this change. */
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "This status change is not allowed."),
    /** The posting reached its headcount before the student answered the offer. */
    POSITION_FILLED(HttpStatus.CONFLICT, "Sorry, this position has just been filled"),
    /** A review is submitted before the application is Completed (P1). */
    REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "Reviews are only possible on Completed applications"),
    /** The caller already reviewed this application (P1). */
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "You have already reviewed this application"),
    /** Any unexpected error. The details are only logged, never sent to the client. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /** The HTTP status sent with this error. */
    public HttpStatus getStatus() {
        return status;
    }

    /** The message used when the caller does not give a custom one. */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
