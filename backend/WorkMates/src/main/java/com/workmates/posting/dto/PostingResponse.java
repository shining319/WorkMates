package com.workmates.posting.dto;

import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.CloseReason;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.profile.dto.TagResponse;
import com.workmates.profile.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Schema(description = "A job posting")
public class PostingResponse {
    @Schema(description = "Posting ID", example = "42")
    private Long id;
    @Schema(description = "ID of the employer who owns the posting", example = "5")
    private Long employerId;
    @Schema(example = "Barista")
    private String title;
    private JobType jobType;
    @Schema(description = "Hourly rate in EUR", example = "13.50")
    private BigDecimal hourlyRate;
    @Schema(example = "7.5")
    private BigDecimal weeklyHours;
    private Area area;
    private String address;
    @Schema(description = "Null when there is no description")
    private String description;
    private Integer headcount;
    private PostingStatus status;
    @Schema(description = "Null while the posting is open")
    private CloseReason closeReason;
    @Schema(description = "Null while the posting is open")
    private Instant closedAt;
    private Instant createdAt;
    @Schema(description = "Required tags, sorted by name")
    private List<TagResponse> tags;
    @Schema(description = "Work time slots, sorted by day then start")
    private List<TimeSlotDto> timeSlots;

    public static PostingResponse from(Posting posting) {
        PostingResponse response = new PostingResponse();
        response.id = posting.getId();
        response.employerId = posting.getEmployerId();
        response.title = posting.getTitle();
        response.jobType = posting.getJobType();
        response.hourlyRate = posting.getHourlyRate();
        response.weeklyHours = posting.getWeeklyHours();
        response.area = posting.getArea();
        response.address = posting.getAddress();
        response.description = posting.getDescription();
        response.headcount = posting.getHeadcount();
        response.status = posting.getStatus();
        response.closeReason = posting.getCloseReason();
        response.closedAt = posting.getClosedAt();
        response.createdAt = posting.getCreatedAt();
        response.tags = posting.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName)).map(TagResponse::from).toList();
        response.timeSlots = posting.getTimeSlots().stream()
                .sorted(Comparator.comparingInt(TimeSlot::getDayOfWeek).thenComparingInt(TimeSlot::getStartMinute))
                .map(TimeSlotDto::from).toList();
        return response;
    }

    public Long getId() { return id; }
    public Long getEmployerId() { return employerId; }
    public String getTitle() { return title; }
    public JobType getJobType() { return jobType; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public BigDecimal getWeeklyHours() { return weeklyHours; }
    public Area getArea() { return area; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public Integer getHeadcount() { return headcount; }
    public PostingStatus getStatus() { return status; }
    public CloseReason getCloseReason() { return closeReason; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TagResponse> getTags() { return tags; }
    public List<TimeSlotDto> getTimeSlots() { return timeSlots; }
}
