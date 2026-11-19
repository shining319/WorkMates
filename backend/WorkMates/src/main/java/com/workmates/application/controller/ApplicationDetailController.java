package com.workmates.application.controller;

import com.workmates.application.dto.ApplicationDetailResponse;
import com.workmates.application.service.ApplicationDetailService;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Students and employers both call it, so there is no {@code @RequireRole}; the service checks ownership. */
@RestController
@RequestMapping("/api/applications/{id}")
@Tag(name = "Applications", description = "Student applications to postings")
public class ApplicationDetailController {
    private final ApplicationDetailService service;

    public ApplicationDetailController(ApplicationDetailService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Application detail with its status history, oldest first. "
            + "The student who applied also gets the employer's contact details.")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    @ApiResponse(responseCode = "404",
            description = "NOT_FOUND (no such application, or you are neither the student nor the posting's employer)")
    public Result<ApplicationDetailResponse> get(CurrentUser user, @PathVariable Long id) {
        return Result.ok(service.get(user, id));
    }
}
