package com.workmates.application.dto;

import com.workmates.application.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "A status change request")
public class StatusRequest {
    @NotNull
    @Schema(description = "The new status", example = "REVIEWED")
    private ApplicationStatus status;

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
}
