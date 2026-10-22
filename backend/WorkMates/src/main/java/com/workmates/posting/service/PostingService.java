package com.workmates.posting.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.posting.dto.PostingRequest;
import com.workmates.posting.dto.PostingResponse;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.service.TagService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates job postings. */
@Service
public class PostingService {
    private final PostingRepository postings;
    private final TagService tags;

    public PostingService(PostingRepository postings, TagService tags) {
        this.postings = postings;
        this.tags = tags;
    }

    /** Saves a new posting for the employer. It starts as {@code OPEN}. */
    @Transactional
    public PostingResponse create(Long employerId, PostingRequest request) {
        Posting posting = new Posting();
        posting.setEmployerId(employerId);
        posting.setStatus(PostingStatus.OPEN);
        fill(posting, request);
        return PostingResponse.from(postings.save(posting));
    }

    /** Checks the whole request, then copies it onto the posting. Nothing is changed when a check fails. */
    private void fill(Posting posting, PostingRequest request) {
        List<String> tagNames = tags.normalizeAll(request.getTags());
        if (tagNames.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "At least one tag is required");
        }
        List<TimeSlot> slots = TimeSlotDto.toSlots(request.getTimeSlots());
        PostingValidator.validate(request.getWeeklyHours(), slots);

        posting.setTitle(request.getTitle());
        posting.setJobType(request.getJobType());
        posting.setHourlyRate(request.getHourlyRate());
        posting.setWeeklyHours(request.getWeeklyHours());
        posting.setArea(request.getArea());
        posting.setAddress(request.getAddress());
        posting.setDescription(blankToNull(request.getDescription()));
        posting.setHeadcount(request.getHeadcount());
        posting.getTags().clear();
        posting.getTags().addAll(tags.findOrCreate(tagNames));
        posting.getTimeSlots().clear();
        posting.getTimeSlots().addAll(slots);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
