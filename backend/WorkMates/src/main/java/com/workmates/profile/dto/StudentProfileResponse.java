package com.workmates.profile.dto;

import com.workmates.common.timeslot.TimeSlotDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "A student's profile with its completeness")
public class StudentProfileResponse {
    @Schema(description = "Name, from the account")
    private String name;
    @Schema(description = "Phone, null if not set")
    private String phone;
    @Schema(description = "Programme, null if not set")
    private String major;
    @Schema(description = "Year of study, null if not set")
    private String yearOfStudy;
    @Schema(description = "Expected hourly rate in EUR, null if not set", example = "13.50")
    private BigDecimal expectedHourlyRate;
    @Schema(description = "Skill and language tags, ordered by name")
    private List<TagResponse> tags;
    @Schema(description = "Weekly free time slots, ordered by day and start")
    private List<TimeSlotDto> timeSlots;
    @Schema(description = "True when there is at least one tag, an expected rate and at least one time slot")
    private boolean complete;
    @Schema(description = "What is missing, any of: tags, expectedHourlyRate, timeSlots", example = "[\"timeSlots\"]")
    private List<String> missing;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public BigDecimal getExpectedHourlyRate() { return expectedHourlyRate; }
    public void setExpectedHourlyRate(BigDecimal expectedHourlyRate) { this.expectedHourlyRate = expectedHourlyRate; }
    public List<TagResponse> getTags() { return tags; }
    public void setTags(List<TagResponse> tags) { this.tags = tags; }
    public List<TimeSlotDto> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotDto> timeSlots) { this.timeSlots = timeSlots; }
    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }
    public List<String> getMissing() { return missing; }
    public void setMissing(List<String> missing) { this.missing = missing; }
}
