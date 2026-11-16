package com.workmates.application.dto;

import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "One row of the student's applications list")
public class MyApplicationResponse {
    @Schema(description = "Application ID", example = "7")
    private Long id;
    @Schema(description = "ID of the posting applied to", example = "42")
    private Long postingId;
    @Schema(example = "Barista")
    private String postingTitle;
    @Schema(description = "Employer display name", example = "Riverside Cafe")
    private String employerName;
    private ApplicationStatus status;
    @Schema(description = "POSITION_FILLED when the system rejected it because the posting filled. Else null")
    private ApplicationStatusReason statusReason;
    @Schema(description = "When the student applied")
    private Instant createdAt;
    @Schema(description = "When the status last changed")
    private Instant updatedAt;

    public static MyApplicationResponse from(Application application, String postingTitle, String employerName) {
        MyApplicationResponse response = new MyApplicationResponse();
        response.id = application.getId();
        response.postingId = application.getPostingId();
        response.postingTitle = postingTitle;
        response.employerName = employerName;
        response.status = application.getStatus();
        response.statusReason = application.getStatusReason();
        response.createdAt = application.getCreatedAt();
        response.updatedAt = application.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getPostingId() { return postingId; }
    public String getPostingTitle() { return postingTitle; }
    public String getEmployerName() { return employerName; }
    public ApplicationStatus getStatus() { return status; }
    public ApplicationStatusReason getStatusReason() { return statusReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
