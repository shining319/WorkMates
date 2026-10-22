package com.workmates.application.entity;

/** Where an application is in its life. Only the state machine may change it (US 4.x). */
public enum ApplicationStatus {
    APPLIED, REVIEWED, INTERVIEW, OFFER, ACCEPTED, DECLINED, REJECTED, COMPLETED
}
