package com.workmates.application.dto;

import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.ApplicationStatus;

/** What one call of {@code ApplicationStateMachine.transition} changed. The caller uses it to send notifications. */
public record StatusChange(Long applicationId, ApplicationStatus from, ApplicationStatus to, ActorType actor) {
}
