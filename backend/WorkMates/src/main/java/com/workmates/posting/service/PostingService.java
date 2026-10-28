package com.workmates.posting.service;

import com.workmates.application.dto.PostingCounts;
import com.workmates.application.service.ApplicationQueryService;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.common.timeslot.TimeSlotDto;
import com.workmates.posting.dto.PostingDetailResponse;
import com.workmates.posting.dto.PostingListItemResponse;
import com.workmates.posting.dto.PostingRequest;
import com.workmates.posting.dto.PostingResponse;
import com.workmates.posting.entity.CloseReason;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import com.workmates.profile.service.TagService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Creates, reads, edits and closes job postings. */
@Service
public class PostingService {
    static final int PAGE_SIZE = 20;
    /** Newest first; the ID breaks ties so that pages never overlap. */
    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PostingRepository postings;
    private final TagService tags;
    private final ApplicationQueryService applications;
    private final EmployerProfileRepository employers;

    public PostingService(PostingRepository postings, TagService tags, ApplicationQueryService applications,
                          EmployerProfileRepository employers) {
        this.postings = postings;
        this.tags = tags;
        this.applications = applications;
        this.employers = employers;
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

    /** A posting with its employer and application counts. Closed postings can be read too. */
    @Transactional(readOnly = true)
    public PostingDetailResponse get(Long postingId) {
        Posting posting = postings.findById(postingId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        EmployerProfile employer = employers.findById(posting.getEmployerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return PostingDetailResponse.from(posting, employer, applications.countsOf(postingId));
    }

    /** One page of the employer's postings in one status, newest first, with application counts. */
    @Transactional(readOnly = true)
    public Page<PostingListItemResponse> mine(Long employerId, PostingStatus status, int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page must be 0 or more");
        }
        Page<Posting> found = postings.findByEmployerIdAndStatus(employerId, status,
                PageRequest.of(page, PAGE_SIZE, NEWEST_FIRST));
        Map<Long, PostingCounts> counts = applications.countsByPostings(
                found.getContent().stream().map(Posting::getId).toList());
        return found.map(posting -> PostingListItemResponse.from(posting, counts.get(posting.getId())));
    }

    /** Edits an open posting of the employer. Tags and time slots are replaced as a whole. */
    @Transactional
    public PostingResponse update(Long employerId, Long postingId, PostingRequest request) {
        Posting posting = lockOwnedOpen(postingId, employerId);
        PostingCounts counts = applications.countsOf(postingId);
        if (request.getHeadcount() < counts.hired()) {
            throw new BusinessException(ErrorCode.HEADCOUNT_BELOW_FILLED,
                    ErrorCode.HEADCOUNT_BELOW_FILLED.getDefaultMessage() + " (" + counts.hired() + ")");
        }
        fill(posting, request);
        // TODO(B4.5.2): when the new headcount equals counts.hired(), close the posting as FILLED (closeIfFilled).
        return PostingResponse.from(posting);
    }

    /** Closes an open posting by hand. Applications in progress are not changed. */
    @Transactional
    public PostingResponse close(Long employerId, Long postingId) {
        Posting posting = lockOwnedOpen(postingId, employerId);
        posting.setStatus(PostingStatus.CLOSED);
        posting.setCloseReason(CloseReason.MANUAL);
        posting.setClosedAt(Instant.now());
        return PostingResponse.from(posting);
    }

    /**
     * Loads the posting with {@code SELECT ... FOR UPDATE}. The row stays locked until the transaction ends.
     * Every write that can change a posting or its applications calls this first (architecture section 8.3).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Posting lockPosting(Long postingId) {
        return postings.findByIdForUpdate(postingId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /** Locks the posting, then checks that the employer owns it (else 404, like a missing one) and it is open. */
    private Posting lockOwnedOpen(Long postingId, Long employerId) {
        Posting posting = lockPosting(postingId);
        if (!posting.getEmployerId().equals(employerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (posting.getStatus() != PostingStatus.OPEN) {
            throw new BusinessException(ErrorCode.POSTING_NOT_EDITABLE);
        }
        return posting;
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
