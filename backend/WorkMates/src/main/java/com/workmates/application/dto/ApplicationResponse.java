package com.workmates.application.dto;

import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "An application")
public class ApplicationResponse {
    @Schema(description = "Application ID", example = "7")
    private Long id;
    @Schema(description = "ID of the posting applied to", example = "42")
    private Long postingId;
    private ApplicationStatus status;
    @Schema(description = "When the student applied")
    private Instant createdAt;

    public static ApplicationResponse from(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.id = application.getId();
        response.postingId = application.getPostingId();
        response.status = application.getStatus();
        response.createdAt = application.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getPostingId() { return postingId; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
