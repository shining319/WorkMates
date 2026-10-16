package com.workmates.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Schema(description = "Login request")
public class LoginRequest {
	@NotBlank
	@Email
	@Schema(description = "Registered email")
	private String email;
	@NotBlank
	@Schema(description = "Account password")
	private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String value) {
		this.email = value;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String value) {
		this.password = value;
	}

}
