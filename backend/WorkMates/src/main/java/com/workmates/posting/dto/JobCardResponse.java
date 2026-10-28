package com.workmates.posting.dto;

import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "One job card in the student's job list")
public class JobCardResponse {
    @Schema(description = "Posting ID", example = "42")
    private Long id;
    @Schema(example = "Barista")
    private String title;
    @Schema(description = "The employer's display name", example = "Café Uno")
    private String employerName;
    @Schema(description = "Job type")
    private JobType jobType;
    @Schema(description = "Hourly rate in EUR", example = "13.50")
    private BigDecimal hourlyRate;
    @Schema(description = "Hours per week", example = "12.5")
    private BigDecimal weeklyHours;
    @Schema(description = "Location area")
    private Area area;
    @Schema(description = "When the posting was created")
    private Instant createdAt;

    public static JobCardResponse from(Posting posting, String employerName) {
        JobCardResponse response = new JobCardResponse();
        response.id = posting.getId();
        response.title = posting.getTitle();
        response.employerName = employerName;
        response.jobType = posting.getJobType();
        response.hourlyRate = posting.getHourlyRate();
        response.weeklyHours = posting.getWeeklyHours();
        response.area = posting.getArea();
        response.createdAt = posting.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getEmployerName() { return employerName; }
    public JobType getJobType() { return jobType; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public BigDecimal getWeeklyHours() { return weeklyHours; }
    public Area getArea() { return area; }
    public Instant getCreatedAt() { return createdAt; }
}
