package com.workmates.application.controller;

import com.workmates.application.dto.ApplicationResponse;
import com.workmates.application.dto.StatusRequest;
import com.workmates.application.service.ApplicationService;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{id}/status")
@Tag(name = "Applications", description = "Student applications to postings")
public class ApplicationStatusController {
    private final ApplicationService service;

    public ApplicationStatusController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Change an application's status. Employers advance or reject; students accept or decline an offer.")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND (no such application, or it is not yours)")
    @ApiResponse(responseCode = "409", description = "INVALID_STATUS_TRANSITION (message has the current status) / POSITION_FILLED")
    public Result<ApplicationResponse> changeStatus(CurrentUser user, @PathVariable Long id,
                                                    @Valid @RequestBody StatusRequest request) {
        return Result.ok(service.changeStatus(user, id, request.getStatus()));
    }
}
