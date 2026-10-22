package com.workmates.posting.dto;

import com.workmates.application.dto.PostingCounts;
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

    public String getEmployerName() { return employerName; }
    public String getEmployerBio() { return employerBio; }
    public long getHiredCount() { return hiredCount; }
    public long getInProgressCount() { return inProgressCount; }
    public long getAutoRejectedCount() { return autoRejectedCount; }
}
