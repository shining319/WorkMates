package com.workmates.application.service;

import com.workmates.application.dto.MyApplicationResponse;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationGroup;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The student's own applications list (US 4.6). */
@Service
public class MyApplicationsService {
    static final int PAGE_SIZE = 20;
    /** Most recently updated first; the ID breaks ties so that pages never overlap. */
    static final Sort LATEST_UPDATE_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

    private final ApplicationRepository applications;
    private final PostingRepository postings;
    private final EmployerProfileRepository employers;

    public MyApplicationsService(ApplicationRepository applications, PostingRepository postings,
                                 EmployerProfileRepository employers) {
        this.applications = applications;
        this.postings = postings;
        this.employers = employers;
    }

    /** One page of the student's applications, optionally only one status group. A null group means all. */
    @Transactional(readOnly = true)
    public Page<MyApplicationResponse> mine(Long studentId, ApplicationGroup group, int page) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page must be 0 or more");
        }
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, LATEST_UPDATE_FIRST);
        Page<Application> found = group == null
                ? applications.findByStudentId(studentId, pageable)
                : applications.findByStudentIdAndStatusIn(studentId, group.getStatuses(), pageable);

        Map<Long, Posting> postingById = postings.findAllById(
                        found.getContent().stream().map(Application::getPostingId).distinct().toList())
                .stream().collect(Collectors.toMap(Posting::getId, Function.identity()));
        List<Long> employerIds = postingById.values().stream().map(Posting::getEmployerId).distinct().toList();
        Map<Long, String> employerNames = employers.findAllById(employerIds).stream()
                .collect(Collectors.toMap(EmployerProfile::getUserId, EmployerProfile::getDisplayName));

        return found.map(application -> {
            Posting posting = postingById.get(application.getPostingId());
            return MyApplicationResponse.from(application, posting.getTitle(),
                    employerNames.get(posting.getEmployerId()));
        });
    }
}
