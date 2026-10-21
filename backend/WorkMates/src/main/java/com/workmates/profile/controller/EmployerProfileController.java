package com.workmates.profile.controller;

import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
import com.workmates.profile.dto.EmployerProfileRequest;
import com.workmates.profile.dto.EmployerProfileResponse;
import com.workmates.profile.service.EmployerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employers/me/profile")
@RequireRole(Role.EMPLOYER)
@Tag(name = "Employer profile", description = "The logged-in employer's profile")
public class EmployerProfileController {

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Employer profile response")
    private static class EmployerProfileResult extends Result<EmployerProfileResponse> {
        @Schema(description = "The employer's profile")
        @Override
        public EmployerProfileResponse getData() {
            return super.getData();
        }
    }

    private final EmployerProfileService service;

    public EmployerProfileController(EmployerProfileService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get my employer profile")
    @ApiResponse(responseCode = "200", description = "The profile",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployerProfileResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not an employer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<EmployerProfileResponse> get(CurrentUser user) {
        return Result.ok(service.get(user.getId()));
    }

    @PutMapping
    @Operation(summary = "Save my employer profile")
    @ApiResponse(responseCode = "200", description = "The saved profile",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployerProfileResult.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not an employer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<EmployerProfileResponse> update(CurrentUser user, @Valid @RequestBody EmployerProfileRequest request) {
        return Result.ok(service.update(user.getId(), request));
    }
}
