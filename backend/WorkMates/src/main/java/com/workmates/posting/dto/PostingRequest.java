package com.workmates.posting.dto;

import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "All fields of a job posting. Used to create a posting and, later, to edit one.")
public class PostingRequest {
    @NotBlank
    @Size(max = 100)
    @Schema(description = "Title, up to 100 characters", example = "Barista", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull
    @Schema(description = "Job type", example = "PART_TIME", requiredMode = Schema.RequiredMode.REQUIRED)
    private JobType jobType;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 6, fraction = 2)
    @Schema(description = "Hourly rate in EUR, above 0, at most 2 decimals", example = "13.50",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal hourlyRate;

    @NotNull
    @DecimalMin("0.5")
    @Digits(integer = 3, fraction = 1)
    @Schema(description = "Weekly hours, a multiple of 0.5, not more than the total length of the time slots",
            example = "7.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal weeklyHours;

    @NotNull
    @Schema(description = "Location area", example = "ATHLONE", requiredMode = Schema.RequiredMode.REQUIRED)
    private Area area;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Address line, up to 200 characters", example = "12 Church Street",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @Size(max = 2000)
    @Schema(description = "Optional description, up to 2000 characters")
    private String description;

    @NotNull
    @Min(1)
    @Schema(description = "Number of places, a whole number from 1", example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer headcount;

    @NotNull
    @Schema(description = "Required skill and language tags, at least 1; each up to 50 characters, duplicates ignored",
            example = "[\"english\", \"customer service\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> tags = new ArrayList<>();

    @NotNull
    @Valid
    @Schema(description = "Weekly work time slots, 1 to 21, 30-minute steps, no overlaps",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TimeSlotDto> timeSlots = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? null : title.trim(); }
    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public BigDecimal getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(BigDecimal weeklyHours) { this.weeklyHours = weeklyHours; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address == null ? null : address.trim(); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getHeadcount() { return headcount; }
    public void setHeadcount(Integer headcount) { this.headcount = headcount; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<TimeSlotDto> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotDto> timeSlots) { this.timeSlots = timeSlots; }
}
