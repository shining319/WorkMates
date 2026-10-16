package com.workmates.account.controller;

import com.workmates.account.dto.*;
import com.workmates.account.service.AccountService;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Register student/employer accounts, log in and read the logged-in user")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AccountService service;

	public AuthController(AccountService service) {
		this.service = service;
	}

	@Operation(summary = "Register and immediately sign in", description = "Student addresses must end in @student.tus.ie; employer addresses may not use that domain.")
	@ApiResponse(responseCode = "201", description = "Registered and JWT issued")
	@ApiResponse(responseCode = "400", description = "VALIDATION_FAILED / INVALID_EMAIL_DOMAIN / STUDENT_EMAIL_FOR_EMPLOYER")
	@ApiResponse(responseCode = "409", description = "EMAIL_TAKEN")
	@PostMapping("/register")
	public ResponseEntity<Result<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(service.register(request)));
	}

	@Operation(summary = "Log in", description = "Returns Result containing a JWT token and UserResponse")
	@ApiResponse(responseCode = "200", description = "JWT and user")
	@ApiResponse(responseCode = "401", description = "BAD_CREDENTIALS")
	@PostMapping("/login")
	public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return Result.ok(service.login(request));
	}

	@Operation(summary = "Get current user")
	@ApiResponse(responseCode = "200", description = "Current authenticated user")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@GetMapping("/me")
	public Result<UserResponse> me(CurrentUser user) {
		return Result.ok(service.me(user));
	}
}
