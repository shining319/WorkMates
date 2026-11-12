package com.workmates.notification.service;

import java.util.Locale;

/**
 * The short English text of each notification type. The text names the job title and, where it applies,
 * the new status. The UI is English only, so there are no message templates to configure.
 *
 * <p>Titles and names are cut to {@link #MAX_PART_LENGTH} characters. The longest fixed text is under 90
 * characters, so a message with two cut parts stays under the 300 characters the database allows.
 */
public final class NotificationMessages {
    static final int MAX_PART_LENGTH = 100;

    private NotificationMessages() {
    }

    /** To the employer. */
    public static String applicationReceived(String studentName, String title) {
        return cut(studentName) + " applied for " + cut(title);
    }

    /** To the student. {@code newStatus} is the status name, for example {@code INTERVIEW}. */
    public static String statusChanged(String title, String newStatus) {
        return cut(title) + ": your application moved to " + capitalize(newStatus);
    }

    /** To the employer. */
    public static String offerAccepted(String studentName, String title) {
        return cut(studentName) + " accepted the offer for " + cut(title);
    }

    /** To the employer. */
    public static String offerDeclined(String studentName, String title) {
        return cut(studentName) + " declined the offer for " + cut(title);
    }

    /** To the other side: the one who did not mark the application as completed. */
    public static String applicationCompleted(String title) {
        return cut(title) + ": the application was marked as completed";
    }

    /** To each student whose application the system rejected because the posting filled. */
    public static String positionFilled(String title) {
        return cut(title) + ": this position has been filled, so your application was rejected";
    }

    /** To the employer. */
    public static String postingFilled(String title) {
        return cut(title) + " is filled and closed";
    }

    private static String cut(String text) {
        String trimmed = text.trim();
        return trimmed.length() <= MAX_PART_LENGTH ? trimmed : trimmed.substring(0, MAX_PART_LENGTH - 1) + "…";
    }

    private static String capitalize(String status) {
        String lower = status.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
