package com.workmates.application.service;

import com.workmates.application.dto.PostingCounts;
import com.workmates.application.repository.ApplicationRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only application queries that the posting package needs. */
@Service
public class ApplicationQueryService {
    private final ApplicationRepository applications;

    public ApplicationQueryService(ApplicationRepository applications) {
        this.applications = applications;
    }

    /** One query for all postings. Every given ID is in the result; a posting without applications has zeros. */
    @Transactional(readOnly = true)
    public Map<Long, PostingCounts> countsByPostings(Collection<Long> postingIds) {
        Map<Long, PostingCounts> counts = new HashMap<>();
        if (!postingIds.isEmpty()) {
            applications.countByPostings(postingIds).forEach(c -> counts.put(c.postingId(), c));
        }
        postingIds.forEach(id -> counts.computeIfAbsent(id, PostingCounts::empty));
        return counts;
    }

    public PostingCounts countsOf(Long postingId) {
        return countsByPostings(List.of(postingId)).get(postingId);
    }
}
