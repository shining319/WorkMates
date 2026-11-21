package com.workmates.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotificationMessagesTest {
    private static final int DATABASE_LIMIT = 300;

    @Test
    void applicationReceivedNamesStudentAndJob() {
        assertEquals("Ann Murphy applied for Barista",
                NotificationMessages.applicationReceived("Ann Murphy", "Barista"));
    }

    @Test
    void statusChangedShowsTheNewStatusInWords() {
        assertEquals("Barista: your application moved to Interview",
                NotificationMessages.statusChanged("Barista", "INTERVIEW"));
        assertEquals("Barista: your application moved to Offer",
                NotificationMessages.statusChanged("Barista", "OFFER"));
        assertEquals("Barista: your application moved to Rejected",
                NotificationMessages.statusChanged("Barista", "REJECTED"));
    }

    @Test
    void offerReplies() {
        assertEquals("Ann Murphy accepted the offer for Barista",
                NotificationMessages.offerAccepted("Ann Murphy", "Barista"));
        assertEquals("Ann Murphy declined the offer for Barista",
                NotificationMessages.offerDeclined("Ann Murphy", "Barista"));
    }

    @Test
    void applicationCompleted() {
        assertEquals("Barista: the application was marked as completed",
                NotificationMessages.applicationCompleted("Barista"));
    }

    @Test
    void filledPostingMessages() {
        assertEquals("Barista: this position has been filled, so your application was rejected",
                NotificationMessages.positionFilled("Barista"));
        assertEquals("Barista is filled and closed", NotificationMessages.postingFilled("Barista"));
    }

    @Test
    void trimsSpacesAroundTitleAndName() {
        assertEquals("Ann applied for Barista", NotificationMessages.applicationReceived("  Ann ", " Barista  "));
    }

    @Test
    void longTitleIsCutButTheRestOfTheSentenceStays() {
        String message = NotificationMessages.statusChanged("T".repeat(500), "OFFER");

        assertTrue(message.length() <= DATABASE_LIMIT);
        assertTrue(message.startsWith("T".repeat(99) + "…: "));
        assertTrue(message.endsWith("your application moved to Offer"));
    }

    @Test
    void everyMessageStaysWithinTheDatabaseLimitWithLongInput() {
        String name = "N".repeat(500);
        String title = "T".repeat(500);

        for (String message : new String[] {
                NotificationMessages.applicationReceived(name, title),
                NotificationMessages.statusChanged(title, "INTERVIEW"),
                NotificationMessages.offerAccepted(name, title),
                NotificationMessages.offerDeclined(name, title),
                NotificationMessages.applicationCompleted(title),
                NotificationMessages.positionFilled(title),
                NotificationMessages.postingFilled(title)}) {
            assertTrue(message.length() <= DATABASE_LIMIT, message);
        }
    }

    @Test
    void shortTitleIsNotChanged() {
        String title = "T".repeat(NotificationMessages.MAX_PART_LENGTH);

        assertEquals(title + " is filled and closed", NotificationMessages.postingFilled(title));
    }
}
