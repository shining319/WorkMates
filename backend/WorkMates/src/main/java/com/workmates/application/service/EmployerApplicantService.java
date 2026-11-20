package com.workmates.application.service;

import com.workmates.account.entity.User;
import com.workmates.account.repository.UserRepository;
import com.workmates.application.dto.EmployerApplicantDetailResponse;
import com.workmates.application.dto.EmployerApplicantRowResponse;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.matching.MatchScoreCalculator;
import com.workmates.posting.dto.MatchBreakdownResponse;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.service.StudentProfileService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US 4.2: employer-only applicant queries. The posting ownership check runs before reading applicants. */
@Service
public class EmployerApplicantService {
    public static final int PAGE_SIZE = 20;

    private final PostingRepository postings;
    private final ApplicationRepository applications;
    private final UserRepository users;
    private final StudentProfileService students;

    public EmployerApplicantService(PostingRepository postings, ApplicationRepository applications,
                                    UserRepository users, StudentProfileService students) {
        this.postings = postings;
        this.applications = applications;
        this.users = users;
        this.students = students;
    }

    /**
     * Filter and sort the entire applicant set BEFORE pagination. Sorting by the exact (not rounded)
     * match score ensures a later page never contains a higher-ranked applicant than an earlier page.
     * The score is recalculated using the student's current profile on every request.
     * {@code statuses} keeps applications in any of the given statuses; null or empty means all.
     */
    @Transactional(readOnly = true)
    public Page<EmployerApplicantRowResponse> list(Long employerId, Long postingId, List<ApplicationStatus> statuses,
                                                   String sortBy, String direction, int page) {
        validatePaging(page, sortBy, direction);
        Posting posting = ownedPosting(employerId, postingId);
        boolean sortByMatch = sortBy.equalsIgnoreCase("matchScore");
        boolean ascending = direction.equalsIgnoreCase("asc");

        List<Application> filtered = applications.findByPostingId(postingId).stream()
                .filter(a -> statuses == null || statuses.isEmpty() || statuses.contains(a.getStatus()))
                .toList();
        if (filtered.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, PAGE_SIZE), 0);
        }

        Map<Long, User> usersById = users.findAllById(filtered.stream()
                .map(Application::getStudentId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<ScoredApplicant> scored = new ArrayList<>();
        for (Application application : filtered) {
            User student = usersById.get(application.getStudentId());
            if (student == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            var match = MatchScoreCalculator.calculate(students.matchInput(student.getId()), posting);
            scored.add(new ScoredApplicant(application, student, match.overallScore(), match.matchPercent()));
        }

        // Ties always use newest application first, then highest ID for deterministic pages.
        Comparator<ScoredApplicant> newestFirst = Comparator
                .comparing((ScoredApplicant item) -> item.application().getCreatedAt()).reversed()
                .thenComparing(item -> item.application().getId(), Comparator.reverseOrder());
        Comparator<ScoredApplicant> order;
        if (sortByMatch) {
            order = Comparator.comparingDouble(ScoredApplicant::exactScore);
            if (!ascending) order = order.reversed();
            order = order.thenComparing(newestFirst);
        } else {
            order = Comparator.comparing((ScoredApplicant item) -> item.application().getCreatedAt());
            if (!ascending) order = order.reversed();
            order = order.thenComparing(item -> item.application().getId(), Comparator.reverseOrder());
        }
        scored.sort(order);

        // Use long arithmetic to avoid int overflow on a very large page argument.
        long start = (long) page * PAGE_SIZE;
        if (start >= scored.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, PAGE_SIZE), scored.size());
        }
        int from = (int) start;
        int to = Math.min(from + PAGE_SIZE, scored.size());
        List<EmployerApplicantRowResponse> result = scored.subList(from, to).stream()
                .map(item -> new EmployerApplicantRowResponse(
                        item.application().getId(), item.student().getId(), item.student().getName(),
                        students.get(item.student().getId()).getMajor(), item.matchPercent(), item.application().getStatus(), item.application().getCreatedAt()))
                .toList();
        return new PageImpl<>(result, PageRequest.of(page, PAGE_SIZE), scored.size());
    }

    /** Displays live profile details and the same match breakdown used by US 3.4. */
    @Transactional(readOnly = true)
    public EmployerApplicantDetailResponse detail(Long employerId, Long postingId, Long applicationId) {
        Posting posting = ownedPosting(employerId, postingId);
        Application application = applications.findById(applicationId)
                .filter(a -> a.getPostingId().equals(postingId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        User student = users.findById(application.getStudentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        StudentProfileResponse profile = students.get(student.getId());
        var match = MatchScoreCalculator.calculate(students.matchInput(student.getId()), posting);
        return new EmployerApplicantDetailResponse(
                application.getId(), postingId, student.getId(), student.getName(),
                student.getEmail(), profile.getPhone(), application.getStatus(), application.getCreatedAt(),
                profile, MatchBreakdownResponse.from(match));
    }

    private Posting ownedPosting(Long employerId, Long postingId) {
        Posting posting = postings.findById(postingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!employerId.equals(posting.getEmployerId())) {
            // Consistent with PostingService: do not reveal another employer's posting/applicants.
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return posting;
    }

    private void validatePaging(int page, String sortBy, String direction) {
        if (page < 0) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page must be 0 or more");
        if (sortBy == null || !(sortBy.equalsIgnoreCase("matchScore")
                || sortBy.equalsIgnoreCase("applicationDate"))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "sortBy must be matchScore or applicationDate");
        }
        if (direction == null || !(direction.equalsIgnoreCase("asc")
                || direction.equalsIgnoreCase("desc"))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "direction must be asc or desc");
        }
    }

    private record ScoredApplicant(Application application, User student, double exactScore, int matchPercent) { }
}
