package com.workmates.posting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.workmates.application.dto.PostingCounts;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.common.timeslot.TimeSlotDto;
import java.util.List;
import com.workmates.posting.entity.Posting;
import com.workmates.profile.entity.EmployerProfile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A posting with its employer and application counts")
public class PostingDetailResponse extends PostingResponse {
    @Schema(description = "The employer's display name", example = "Brew & Co")
    private String employerName;
    @Schema(description = "The employer's bio. Null when there is none.")
    private String employerBio;
    @Schema(description = "Accepted + Completed applications: the places already taken", example = "1")
    private long hiredCount;
    @Schema(description = "Applied, Reviewed, Interview and Offer applications", example = "3")
    private long inProgressCount;
    @Schema(description = "Applications the system rejected because the posting filled", example = "0")
    private long autoRejectedCount;
    /** These US 3.4 fields are populated for student requests only. */
    @Schema(description = "Student match explanation; null for employer views")
    private MatchBreakdownResponse matchBreakdown;
    private List<TimeSlotDto> studentFreeSlots;
    private Boolean alreadyApplied;
    private ApplicationStatus applicationStatus;
    private Boolean canApply;
    private String closedNotice;

    public static PostingDetailResponse from(Posting posting, EmployerProfile employer, PostingCounts counts) {
        PostingDetailResponse response = new PostingDetailResponse();
        response.copyFrom(posting);
        response.employerName = employer.getDisplayName();
        response.employerBio = employer.getBio();
        response.hiredCount = counts.hired();
        response.inProgressCount = counts.inProgress();
        response.autoRejectedCount = counts.autoRejected();
        return response;
    }

    public void addStudentMatch(MatchBreakdownResponse match, List<TimeSlotDto> freeSlots,
                                ApplicationStatus currentStatus, boolean applied, boolean open) {
        this.matchBreakdown = match;
        this.studentFreeSlots = freeSlots;
        this.applicationStatus = currentStatus;
        this.alreadyApplied = applied;
        this.canApply = open && !applied;
        this.closedNotice = open ? null : "This posting is closed";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public MatchBreakdownResponse getMatchBreakdown() { return matchBreakdown; }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<TimeSlotDto> getStudentFreeSlots() { return studentFreeSlots; }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getAlreadyApplied() { return alreadyApplied; }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ApplicationStatus getApplicationStatus() { return applicationStatus; }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean getCanApply() { return canApply; }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getClosedNotice() { return closedNotice; }
    public String getEmployerName() { return employerName; }
    public String getEmployerBio() { return employerBio; }
    public long getHiredCount() { return hiredCount; }
    public long getInProgressCount() { return inProgressCount; }
    public long getAutoRejectedCount() { return autoRejectedCount; }
}
