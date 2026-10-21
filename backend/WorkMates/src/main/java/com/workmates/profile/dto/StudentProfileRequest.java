package com.workmates.profile.dto;

import com.workmates.common.timeslot.TimeSlotDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "The whole student profile. Every save replaces tags and time slots as a group.")
public class StudentProfileRequest {
    @NotBlank
    @Size(max = 100)
    @Schema(description = "Name, up to 100 characters. Also changes the name shown everywhere.", example = "Alex Murphy",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 30)
    @Schema(description = "Phone, up to 30 characters", example = "+353 87 123 4567")
    private String phone;

    @Size(max = 100)
    @Schema(description = "Programme, up to 100 characters", example = "Software Development")
    private String major;

    @Size(max = 30)
    @Schema(description = "Year of study, free text up to 30 characters", example = "Year 2")
    private String yearOfStudy;

    @DecimalMin("0.01")
    @Digits(integer = 6, fraction = 2)
    @Schema(description = "Expected hourly rate in EUR, above 0, at most 2 decimals", example = "13.50")
    private BigDecimal expectedHourlyRate;

    @NotNull
    @Schema(description = "Skill and language tags, up to 20 after duplicates are dropped; each up to 50 characters",
            example = "[\"java\", \"english\"]")
    private List<String> tags = new ArrayList<>();

    @NotNull
    @Valid
    @Schema(description = "Weekly free time slots, up to 21, 30-minute steps, no overlaps")
    private List<TimeSlotDto> timeSlots = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public BigDecimal getExpectedHourlyRate() { return expectedHourlyRate; }
    public void setExpectedHourlyRate(BigDecimal expectedHourlyRate) { this.expectedHourlyRate = expectedHourlyRate; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<TimeSlotDto> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotDto> timeSlots) { this.timeSlots = timeSlots; }
}
