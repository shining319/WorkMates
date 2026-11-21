package com.workmates.application.entity;

import java.util.List;

/** The status groups of the student's "My applications" list (US 4.6). */
public enum ApplicationGroup {
    IN_PROGRESS(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWED, ApplicationStatus.INTERVIEW),
    OFFER(ApplicationStatus.OFFER),
    ACCEPTED(ApplicationStatus.ACCEPTED),
    FINISHED(ApplicationStatus.REJECTED, ApplicationStatus.DECLINED, ApplicationStatus.COMPLETED);

    private final List<ApplicationStatus> statuses;

    ApplicationGroup(ApplicationStatus... statuses) {
        this.statuses = List.of(statuses);
    }

    public List<ApplicationStatus> getStatuses() {
        return statuses;
    }
}
