package com.workmates.notification.entity;

/**
 * What happened. Stored as text in {@code notifications.type}.
 * The P1 types ({@code NEW_MATCHING_JOB}, {@code REVIEW_RECEIVED}) are added with their stories.
 */
public enum NotificationType {
    /** A student applied. Goes to the employer. */
    APPLICATION_RECEIVED,
    /** The employer moved the application to Reviewed, Interview, Offer or Rejected. Goes to the student. */
    STATUS_CHANGED,
    /** The student accepted the offer. Goes to the employer. */
    OFFER_ACCEPTED,
    /** The student declined the offer. Goes to the employer. */
    OFFER_DECLINED,
    /** One side marked the application Completed. Goes to the other side. */
    APPLICATION_COMPLETED,
    /** The system rejected the application because the posting is filled. Goes to the student. */
    POSITION_FILLED,
    /** The posting is filled and closed. Goes to the employer. */
    POSTING_FILLED
}
