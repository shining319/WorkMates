package com.workmates.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An employer's profile")
public class EmployerProfileResponse {
    @Schema(description = "Business or person name", example = "Campus Cafe")
    private String displayName;
    @Schema(description = "Short description, null if not set")
    private String bio;
    @Schema(description = "Contact email", example = "jobs@campuscafe.ie")
    private String contactEmail;
    @Schema(description = "Phone, null if not set")
    private String phone;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
