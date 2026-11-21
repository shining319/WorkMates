package com.workmates.application.controller;

import com.workmates.application.dto.ApplicationResponse;
import com.workmates.application.service.ApplicationService;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postings/{id}/applications")
@Tag(name = "Applications", description = "Student applications to postings")
public class ApplicationController {

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Application response")
    private static class ApplicationResult extends Result<ApplicationResponse> {
        @Schema(description = "The new application")
        @Override
        public ApplicationResponse getData() {
            return super.getData();
        }
    }

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @RequireRole(Role.STUDENT)
    @Operation(summary = "Apply to a posting with my saved profile. The application starts as APPLIED.")
    @ApiResponse(responseCode = "201", description = "The new application",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApplicationResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not a student)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND (no such posting)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "409",
            description = "POSTING_CLOSED / PROFILE_INCOMPLETE (the message lists what is missing) / ALREADY_APPLIED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<ApplicationResponse>> apply(CurrentUser user, @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(service.apply(user.getId(), id)));
    }
}
