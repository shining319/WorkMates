package com.workmates.posting.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.matching.TimeCoverageCalculator;
import com.workmates.posting.dto.PostingResponse;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.service.StudentProfileService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Student search: SQL hard filters, then duration coverage BEFORE paging (US 3.2). */
@Service
public class PostingSearchService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private final PostingRepository postings;
    private final StudentProfileService students;

    public PostingSearchService(PostingRepository postings, StudentProfileService students) {
        this.postings = postings;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public Page<PostingResponse> search(Long studentId, String keyword, List<Area> areas,
            List<JobType> types, BigDecimal minRate, boolean fitsMyTime, int page, int size) {
        validate(page, size, minRate);
        Pageable paging = PageRequest.of(page, size, NEWEST_FIRST);
        Specification<Posting> spec = filters(keyword, areas, types, minRate);

        // Without a time filter the database paginates directly.
        if (!fitsMyTime) {
            Page<Posting> found = postings.findAll(spec, paging);
            fetchCollections(found.getContent());
            return found.map(PostingResponse::from);
        }

        // The student profile is resolved from the authenticated JWT user, NEVER from a client-supplied ID.
        var free = students.matchInput(studentId).getTimeSlots();
        if (free.isEmpty()) return new PageImpl<>(List.of(), paging, 0);

        // Time filtering MUST precede pagination; otherwise totals and result pages would be wrong.
        List<Posting> hardMatches = postings.findAll(spec, NEWEST_FIRST);
        if (hardMatches.isEmpty()) return new PageImpl<>(List.of(), paging, 0);
        postings.fetchSlotsByIds(hardMatches.stream().map(Posting::getId).toList());
        List<Posting> eligible = hardMatches.stream()
                .filter(posting -> TimeCoverageCalculator.fits(posting.getTimeSlots(), free))
                .toList();
        long from = (long) page * size;
        if (from >= eligible.size()) return new PageImpl<>(List.of(), paging, eligible.size());
        int end = (int) Math.min(from + size, eligible.size());
        List<Posting> selected = eligible.subList((int) from, end);
        fetchCollections(selected);
        return new PageImpl<>(selected.stream().map(PostingResponse::from).toList(), paging, eligible.size());
    }

    /** All filter categories are ANDed; selections within an area/type category are ORed. */
    static Specification<Posting> filters(String keyword, List<Area> areas,
            List<JobType> types, BigDecimal minRate) {
        String term = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Area> locations = areas == null ? List.of() : List.copyOf(areas);
        List<JobType> jobTypes = types == null ? List.of() : List.copyOf(types);
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(root.get("status"), PostingStatus.OPEN));
            if (!locations.isEmpty()) conditions.add(root.get("area").in(locations));
            if (!jobTypes.isEmpty()) conditions.add(root.get("jobType").in(jobTypes));
            if (minRate != null) conditions.add(cb.greaterThanOrEqualTo(root.get("hourlyRate"), minRate));
            if (!term.isEmpty()) {
                // Literal percent/underscore/backslash must not turn into wildcard searches.
                String pattern = "%" + term.replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_") + "%";
                Subquery<Long> tagSearch = query.subquery(Long.class);
                Root<Posting> taggedPosting = tagSearch.from(Posting.class);
                Join<Posting, Tag> tag = taggedPosting.join("tags");
                tagSearch.select(taggedPosting.get("id"));
                tagSearch.where(cb.equal(taggedPosting.get("id"), root.get("id")),
                        cb.like(cb.lower(tag.get("name")), pattern, '\\'));
                conditions.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\'), cb.exists(tagSearch)));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    private void fetchCollections(List<Posting> items) {
        if (items.isEmpty()) return;
        List<Long> ids = items.stream().map(Posting::getId).toList();
        postings.fetchTagsByIds(ids);
        postings.fetchSlotsByIds(ids);
    }

    private static void validate(int page, int size, BigDecimal minRate) {
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "page must be 0 or greater and size must be between 1 and 50");
        }
        if (minRate != null && (minRate.signum() < 0 || minRate.scale() > 2)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "minRate must be a non-negative hourly rate with at most two decimal places");
        }
    }
}
