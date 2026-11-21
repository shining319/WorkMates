package com.workmates.account.controller;

import com.workmates.account.dto.AuthResponse;
import com.workmates.account.dto.LoginRequest;
import com.workmates.account.dto.RegisterRequest;
import com.workmates.account.dto.UserResponse;
import com.workmates.account.service.AccountService;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP endpoints used by the frontend's login and registration screens. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration, login, and current-user details")
public class AuthController {

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Token and user response")
    private static class AuthResult extends Result<AuthResponse> {
        @Schema(description = "The JWT and the account it belongs to")
        @Override
        public AuthResponse getData() {
            return super.getData();
        }
    }

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Current user response")
    private static class UserResult extends Result<UserResponse> {
        @Schema(description = "The logged-in account")
        @Override
        public UserResponse getData() {
            return super.getData();
        }
    }

    private final AccountService service;

    public AuthController(AccountService service) { this.service = service; }

    @PostMapping("/register")
    @Operation(summary = "Register and automatically sign in")
    @ApiResponse(responseCode = "201", description = "Account and empty role profile created; token returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResult.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED / INVALID_EMAIL_DOMAIN / STUDENT_EMAIL_FOR_EMPLOYER",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "409", description = "EMAIL_TAKEN",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(service.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    @ApiResponse(responseCode = "200", description = "JWT and user returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResult.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "BAD_CREDENTIALS",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(service.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    @ApiResponse(responseCode = "200", description = "Logged-in account",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<UserResponse> me(CurrentUser user) {
        // The team's CurrentUserArgumentResolver provides this parameter.
        return Result.ok(service.me(user));
    }
}
