package com.workmates.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Schema(description = "Auth response")
public class AuthResponse {
	@Schema(description = "HS256 JWT access token, seven day lifetime")
	private String token;
	@Schema(description = "Current user details")
	private UserResponse user;

	public String getToken() {
		return token;
	}

	public void setToken(String value) {
		this.token = value;
	}

	public UserResponse getUser() {
		return user;
	}

	public void setUser(UserResponse value) {
		this.user = value;
	}

}
