package com.workmates.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Schema(description = "Register request")
public class RegisterRequest {
	@NotBlank
	@Size(max = 100)
	@Schema(description = "Full name, up to 100 characters")
	private String name;
	@NotBlank
	@Email
	@Size(max = 254)
	@Schema(description = "Email address; students must use @student.tus.ie")
	private String email;
	@NotBlank
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$", message = "Password needs at least 8 characters, a letter and a number")
	@Schema(description = "At least eight characters, including a letter and a number")
	private String password;
	@NotBlank
	@Schema(description = "Must match password")
	private String confirmPassword;
	@NotNull
	@Schema(description = "Account role: STUDENT or EMPLOYER")
	private com.workmates.account.entity.Role role;

	public String getName() {
		return name;
	}

	public void setName(String value) {
		this.name = value;
	}

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

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String value) {
		this.confirmPassword = value;
	}

	public com.workmates.account.entity.Role getRole() {
		return role;
	}

	public void setRole(com.workmates.account.entity.Role value) {
		this.role = value;
	}

}
