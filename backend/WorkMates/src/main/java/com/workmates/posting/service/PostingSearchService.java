package com.workmates.posting.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.timeslot.TimeSlot;
import com.workmates.matching.MatchScoreCalculator;
import com.workmates.posting.dto.JobCardResponse;
import com.workmates.posting.entity.Area;
import com.workmates.posting.entity.JobType;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.entity.Tag;
import com.workmates.profile.repository.EmployerProfileRepository;
import com.workmates.profile.service.StudentProfileService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Student job search (architecture section 8.1). SQL does the hard filters; the "fits my time" filter and
 * the paging happen in memory, so that ranking by match score (US 3.3) can use the same path.
 */
@Service
public class PostingSearchService {
    /** "Fits my time" keeps postings whose slots are covered by at least this share. */
    static final double FITS_MY_TIME_MIN = 0.5;
    private static final char ESCAPE = '\\';

    /** The optional filters. They all combine with AND; null or empty means no filter. */
    public record Filters(String keyword, List<Area> areas, List<JobType> types, BigDecimal minRate,
                          boolean fitsMyTime) {
    }

    private final PostingRepository postings;
    private final EmployerProfileRepository employers;
    private final StudentProfileService students;

    public PostingSearchService(PostingRepository postings, EmployerProfileRepository employers,
                                StudentProfileService students) {
        this.postings = postings;
        this.employers = employers;
        this.students = students;
    }

    /** One page of open postings that pass every filter, newest first. */
    @Transactional(readOnly = true)
    public Page<JobCardResponse> search(Long studentId, Filters filters, int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page must be 0 or more");
        }
        List<Posting> found = postings.findAll(specification(filters), PostingService.NEWEST_FIRST);
        if (filters.fitsMyTime()) {
            List<TimeSlot> freeSlots = students.matchInput(studentId).getTimeSlots();
            found = found.stream()
                    .filter(p -> MatchScoreCalculator.timeMatch(freeSlots, p.getTimeSlots()).score() >= FITS_MY_TIME_MIN)
                    .toList();
        }
        int from = (int) Math.min((long) page * PostingService.PAGE_SIZE, found.size());
        List<Posting> onPage = found.subList(from, Math.min(from + PostingService.PAGE_SIZE, found.size()));

        Map<Long, String> employerNames = employers
                .findAllById(onPage.stream().map(Posting::getEmployerId).distinct().toList()).stream()
                .collect(Collectors.toMap(EmployerProfile::getUserId, EmployerProfile::getDisplayName));
        List<JobCardResponse> cards = onPage.stream()
                .map(p -> JobCardResponse.from(p, employerNames.get(p.getEmployerId()))).toList();
        return new PageImpl<>(cards, PageRequest.of(page, PostingService.PAGE_SIZE), found.size());
    }

    private static Specification<Posting> specification(Filters filters) {
        List<Specification<Posting>> parts = new ArrayList<>();
        parts.add((root, query, cb) -> cb.equal(root.get("status"), PostingStatus.OPEN));
        if (filters.keyword() != null && !filters.keyword().isBlank()) {
            parts.add(keyword(filters.keyword()));
        }
        if (filters.areas() != null && !filters.areas().isEmpty()) {
            parts.add((root, query, cb) -> root.get("area").in(filters.areas()));
        }
        if (filters.types() != null && !filters.types().isEmpty()) {
            parts.add((root, query, cb) -> root.get("jobType").in(filters.types()));
        }
        if (filters.minRate() != null) {
            parts.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("hourlyRate"), filters.minRate()));
        }
        return Specification.allOf(parts);
    }

    /** The keyword appears in the title, the description or the name of one of the tags, ignoring case. */
    static Specification<Posting> keyword(String keyword) {
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Subquery<Long> tagMatch = query.subquery(Long.class);
            Root<Posting> inner = tagMatch.correlate(root);
            Join<Posting, Tag> tag = inner.join("tags");
            tagMatch.select(tag.<Long>get("id")).where(cb.like(cb.lower(tag.get("name")), pattern, ESCAPE));
            Predicate title = cb.like(cb.lower(root.get("title")), pattern, ESCAPE);
            Predicate description = cb.like(cb.lower(root.get("description")), pattern, ESCAPE);
            return cb.or(title, description, cb.exists(tagMatch));
        };
    }

    /** Makes {@code %}, {@code _} and the escape character itself plain characters in a LIKE pattern. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
