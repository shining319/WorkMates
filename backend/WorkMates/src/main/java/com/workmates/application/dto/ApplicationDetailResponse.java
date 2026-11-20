package com.workmates.application.dto;

import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusHistory;
import com.workmates.application.entity.ApplicationStatusReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "An application with its status history. The employer's contact details are only for the student.")
public record ApplicationDetailResponse(
        @Schema(description = "Application ID", example = "7") Long id,
        @Schema(description = "ID of the posting applied to", example = "42") Long postingId,
        @Schema(example = "Barista") String postingTitle,
        ApplicationStatus status,
        @Schema(description = "POSITION_FILLED when the system rejected it because the posting filled. Else null")
        ApplicationStatusReason statusReason,
        @Schema(description = "When the student applied") Instant createdAt,
        @Schema(description = "When the status last changed") Instant updatedAt,
        @Schema(description = "Every status change, oldest first, starting with Applied") List<HistoryStep> history,
        @Schema(description = "The employer's contact details. Null when the employer is the caller")
        EmployerContact employerContact) {

    public static ApplicationDetailResponse from(Application application, String postingTitle,
                                                 List<ApplicationStatusHistory> history,
                                                 EmployerContact employerContact) {
        return new ApplicationDetailResponse(application.getId(), application.getPostingId(), postingTitle,
                application.getStatus(), application.getStatusReason(), application.getCreatedAt(),
                application.getUpdatedAt(), history.stream().map(HistoryStep::from).toList(), employerContact);
    }

    @Schema(description = "One status change")
    public record HistoryStep(
            @Schema(description = "Null in the first step, when the application was created")
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            @Schema(description = "Who made the change") ActorType actorType,
            @Schema(description = "POSITION_FILLED when the system rejected it because the posting filled. Else null")
            ApplicationStatusReason reason,
            @Schema(description = "When the change happened") Instant createdAt) {

        static HistoryStep from(ApplicationStatusHistory step) {
            return new HistoryStep(step.getFromStatus(), step.getToStatus(), step.getActorType(),
                    step.getReason(), step.getCreatedAt());
        }
    }

    @Schema(description = "How the student can reach the employer")
    public record EmployerContact(
            @Schema(description = "Employer display name", example = "Riverside Cafe") String name,
            @Schema(example = "jobs@riversidecafe.ie") String contactEmail,
            @Schema(description = "Null if not set", example = "090 640 1234") String phone) {
    }
}
