package com.workmates.posting.service;

import com.workmates.application.dto.PostingCounts;
import com.workmates.application.service.ApplicationQueryService;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.posting.dto.MyPostingResponse;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyPostingsService {
    private static final int PAGE_SIZE = 20;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private final PostingRepository postings;
    private final ApplicationQueryService applications;

    public MyPostingsService(PostingRepository postings, ApplicationQueryService applications) {
        this.postings = postings;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public Page<MyPostingResponse> mine(Long employerId, PostingStatus status, int page) {
        if (status == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "status must be OPEN or CLOSED");
        }
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page must be 0 or greater");
        }
        Page<Posting> matches = postings.findByEmployerIdAndStatus(
                employerId, status, PageRequest.of(page, PAGE_SIZE, NEWEST_FIRST));
        Map<Long, PostingCounts> counts = applications.countsByPostings(
                matches.getContent().stream().map(Posting::getId).toList());
        return matches.map(posting -> new MyPostingResponse(posting, counts.get(posting.getId())));
    }
}
