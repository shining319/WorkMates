package com.workmates.posting.dto;

import com.workmates.application.dto.PostingCounts;
import com.workmates.posting.entity.CloseReason;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "One row of the employer's postings list")
public class PostingListItemResponse {
    @Schema(description = "Posting ID", example = "42")
    private Long id;
    @Schema(example = "Barista")
    private String title;
    @Schema(description = "Job type")
    private JobType jobType;
    @Schema(description = "OPEN or CLOSED")
    private PostingStatus status;
    @Schema(description = "MANUAL or FILLED. Null while the posting is open")
    private CloseReason closeReason;
    @Schema(description = "Number of places", example = "3")
    private Integer headcount;
    @Schema(description = "Accepted + Completed applications: the places already taken", example = "1")
    private long hiredCount;
    @Schema(description = "Applied, Reviewed, Interview and Offer applications", example = "3")
    private long inProgressCount;
    @Schema(description = "When the posting was created")
    private Instant createdAt;

    public static PostingListItemResponse from(Posting posting, PostingCounts counts) {
        PostingListItemResponse response = new PostingListItemResponse();
        response.id = posting.getId();
        response.title = posting.getTitle();
        response.jobType = posting.getJobType();
        response.status = posting.getStatus();
        response.closeReason = posting.getCloseReason();
        response.headcount = posting.getHeadcount();
        response.hiredCount = counts.hired();
        response.inProgressCount = counts.inProgress();
        response.createdAt = posting.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public JobType getJobType() { return jobType; }
    public PostingStatus getStatus() { return status; }
    public CloseReason getCloseReason() { return closeReason; }
    public Integer getHeadcount() { return headcount; }
    public long getHiredCount() { return hiredCount; }
    public long getInProgressCount() { return inProgressCount; }
    public Instant getCreatedAt() { return createdAt; }
}
