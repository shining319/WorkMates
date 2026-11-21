package com.workmates.profile.controller;

import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
import com.workmates.profile.dto.StudentProfileRequest;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.service.StudentProfileService;
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
@RequestMapping("/api/students/me/profile")
@RequireRole(Role.STUDENT)
@Tag(name = "Student profile", description = "The logged-in student's profile")
public class StudentProfileController {

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Student profile response")
    private static class StudentProfileResult extends Result<StudentProfileResponse> {
        @Schema(description = "The student's profile")
        @Override
        public StudentProfileResponse getData() {
            return super.getData();
        }
    }

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get my profile, with whether it is complete and what is missing")
    @ApiResponse(responseCode = "200", description = "The profile",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentProfileResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not a student)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<StudentProfileResponse> get(CurrentUser user) {
        return Result.ok(service.get(user.getId()));
    }

    @PutMapping
    @Operation(summary = "Save my profile (tags and time slots are replaced as a group)")
    @ApiResponse(responseCode = "200", description = "The saved profile",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudentProfileResult.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED / TIME_SLOT_INVALID",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not a student)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<StudentProfileResponse> update(CurrentUser user, @Valid @RequestBody StudentProfileRequest request) {
        return Result.ok(service.update(user.getId(), request));
    }
}
