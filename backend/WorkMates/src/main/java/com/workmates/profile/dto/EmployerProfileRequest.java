package com.workmates.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "The whole employer profile")
public class EmployerProfileRequest {
    @NotBlank
    @Size(max = 100)
    @Schema(description = "Business or person name, up to 100 characters", example = "Campus Cafe",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;

    @Size(max = 1000)
    @Schema(description = "Short description, up to 1000 characters", example = "A small cafe near the main entrance.")
    private String bio;

    @NotBlank
    @Email
    @Size(max = 254)
    @Schema(description = "Contact email, valid format, up to 254 characters", example = "jobs@campuscafe.ie",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactEmail;

    @Size(max = 30)
    @Schema(description = "Phone, up to 30 characters", example = "+353 90 123 4567")
    private String phone;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName == null ? null : displayName.trim(); }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail == null ? null : contactEmail.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
