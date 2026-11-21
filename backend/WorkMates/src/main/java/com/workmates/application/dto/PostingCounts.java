package com.workmates.application.dto;

/**
 * Application counts of one posting.
 *
 * @param hired        {@code ACCEPTED} + {@code COMPLETED}: the places already taken
 * @param inProgress   {@code APPLIED}, {@code REVIEWED}, {@code INTERVIEW} and {@code OFFER}
 * @param autoRejected rejected by the system because the posting filled ({@code POSITION_FILLED})
 */
public record PostingCounts(Long postingId, Long hired, Long inProgress, Long autoRejected) {

    /** The counts of a posting that has no applications. */
    public static PostingCounts empty(Long postingId) {
        return new PostingCounts(postingId, 0L, 0L, 0L);
    }
}
