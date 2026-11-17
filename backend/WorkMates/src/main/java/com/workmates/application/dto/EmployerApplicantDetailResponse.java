package com.workmates.application.dto;

import com.workmates.application.entity.ApplicationStatus;
import com.workmates.posting.dto.MatchBreakdownResponse;
import com.workmates.profile.dto.StudentProfileResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Current applicant profile and match explanation, visible only to the posting owner (US 4.2). */
@Schema(description = "Student details for the employer who owns the posting")
public record EmployerApplicantDetailResponse(
        Long applicationId,
        Long postingId,
        Long studentId,
        String studentName,
        String campusEmail,
        String phone,
        ApplicationStatus status,
        Instant applicationDate,
        StudentProfileResponse profile,
        MatchBreakdownResponse matchBreakdown) {
}
