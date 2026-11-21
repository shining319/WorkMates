package com.workmates.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful registration or login response")
public class AuthResponse {
    @Schema(description = "Signed JWT access token. Sent as Authorization: Bearer <token>.")
    private String token;

    @Schema(description = "Logged-in user's public account details")
    private UserResponse user;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}
