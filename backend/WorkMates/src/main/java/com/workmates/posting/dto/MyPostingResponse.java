package com.workmates.posting.dto;

import com.workmates.application.dto.PostingCounts;
import com.workmates.posting.entity.CloseReason;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** The exact fields consumed by the existing MyPostingsPage frontend. */
@Schema(description = "One employer-owned posting with application counts")
public class MyPostingResponse {
    private final Long id;
    private final String title;
    private final JobType jobType;
    private final PostingStatus status;
    private final CloseReason closeReason;
    private final Integer headcount;
    private final long hiredCount;
    private final long inProgressCount;
    private final Instant createdAt;

    public MyPostingResponse(Posting posting, PostingCounts counts) {
        id = posting.getId();
        title = posting.getTitle();
        jobType = posting.getJobType();
        status = posting.getStatus();
        closeReason = posting.getCloseReason();
        headcount = posting.getHeadcount();
        hiredCount = counts.hired();
        inProgressCount = counts.inProgress();
        createdAt = posting.getCreatedAt();
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
