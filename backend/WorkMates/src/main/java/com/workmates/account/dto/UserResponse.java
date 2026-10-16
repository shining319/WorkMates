package com.workmates.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Schema(description = "User response")
public class UserResponse {
	@Schema(description = "Database user ID")
	private Long id;
	@Schema(description = "Display name")
	private String name;
	@Schema(description = "Normalised email")
	private String email;
	@Schema(description = "STUDENT or EMPLOYER")
	private com.workmates.account.entity.Role role;

	public Long getId() {
		return id;
	}

	public void setId(Long value) {
		this.id = value;
	}

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

	public com.workmates.account.entity.Role getRole() {
		return role;
	}

	public void setRole(com.workmates.account.entity.Role value) {
		this.role = value;
	}

}
