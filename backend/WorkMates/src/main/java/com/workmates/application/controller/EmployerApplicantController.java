package com.workmates.application.controller;

import com.workmates.application.dto.EmployerApplicantDetailResponse;
import com.workmates.application.dto.EmployerApplicantRowResponse;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.service.EmployerApplicantService;
import com.workmates.common.PageInfo;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** US 4.2: employer views and filters applicants. Adds exactly two read-only routes. */
@RestController
@RequestMapping("/api/postings/{postingId}/applicants")
@RequireRole(Role.EMPLOYER)
@Tag(name = "Employer Applicants", description = "US 4.2: applicants to an employer's own postings")
public class EmployerApplicantController {
    private final EmployerApplicantService service;

    public EmployerApplicantController(EmployerApplicantService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List applicants for my posting, filter by one or more statuses, sort by match or date, 20/page")
    public Result<List<EmployerApplicantRowResponse>> list(
            CurrentUser employer,
            @PathVariable Long postingId,
            @Parameter(description = "One or more statuses, e.g. ACCEPTED,COMPLETED. Omit for all")
            @RequestParam(required = false) List<ApplicationStatus> status,
            @Parameter(description = "matchScore or applicationDate")
            @RequestParam(defaultValue = "matchScore") String sortBy,
            @Parameter(description = "desc or asc")
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page) {
        Page<EmployerApplicantRowResponse> found = service.list(
                employer.getId(), postingId, status, sortBy, direction, page);
        // An empty data list is the signal for the UI to show "No applications yet".
        PageInfo pageInfo = new PageInfo(found.getNumber(), found.getSize(),
                found.getTotalElements(), found.getTotalPages());
        return Result.ok(found.getContent(), pageInfo);
    }

    @GetMapping("/{applicationId}")
    @Operation(summary = "View an applicant's current profile, contact details and match explanation")
    public Result<EmployerApplicantDetailResponse> detail(
            CurrentUser employer,
            @PathVariable Long postingId,
            @PathVariable Long applicationId) {
        return Result.ok(service.detail(employer.getId(), postingId, applicationId));
    }
}
