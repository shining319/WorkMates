package com.workmates.account.dto;

import com.workmates.common.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Safe-to-expose account details. No password hash is returned.")
public class UserResponse {
    @Schema(description = "Account ID", example = "42")
    private Long id;
    @Schema(description = "Account holder's name", example = "Alex Murphy")
    private String name;
    @Schema(description = "Normalised lowercase email", example = "alex@student.tus.ie")
    private String email;
    @Schema(description = "Account role", allowableValues = {"STUDENT", "EMPLOYER"})
    private Role role;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
