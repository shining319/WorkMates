package com.workmates.application.dto;

import com.workmates.application.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** One applicant in an employer's posting-specific applicant list (US 4.2). */
@Schema(description = "Applicant summary; current match is recalculated from the student's saved profile")
public record EmployerApplicantRowResponse(
        Long applicationId,
        Long studentId,
        String studentName,
        String major,
        int matchPercent,
        ApplicationStatus status,
        Instant applicationDate) {
}
