package com.workmates.account.dto;

import com.workmates.common.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Registration details for a student or employer")
public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    @Schema(description = "Full name, maximum 100 characters", example = "Alex Murphy", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    @Schema(description = "Student accounts need a @student.tus.ie email. Employer accounts cannot use that domain.",
        example = "alex@student.tus.ie", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$", message = "Password needs at least 8 characters, a letter and a number")
    @Schema(description = "Minimum 8 characters, at least one letter and one number", format = "password",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank
    @Schema(description = "Must match password", format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

    @NotNull
    @Schema(description = "Account type", allowableValues = {"STUDENT", "EMPLOYER"},
        requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
