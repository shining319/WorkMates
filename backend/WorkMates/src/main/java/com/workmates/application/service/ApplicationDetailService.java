package com.workmates.application.service;

import com.workmates.application.dto.ApplicationDetailResponse;
import com.workmates.application.dto.ApplicationDetailResponse.EmployerContact;
import com.workmates.application.entity.Application;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.application.repository.ApplicationStatusHistoryRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.Role;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.repository.PostingRepository;
import com.workmates.profile.dto.EmployerProfileResponse;
import com.workmates.profile.service.EmployerProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One application with its status history (B4.2.2, US 4.6). The student who applied and the employer who owns the
 * posting can read it. Only the student also gets the employer's contact details. The employer's view of the
 * student's profile and match is the applicant detail of US 4.2, so it is not repeated here.
 */
@Service
public class ApplicationDetailService {
    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository history;
    private final PostingRepository postings;
    private final EmployerProfileService employers;

    public ApplicationDetailService(ApplicationRepository applications, ApplicationStatusHistoryRepository history,
                                    PostingRepository postings, EmployerProfileService employers) {
        this.applications = applications;
        this.history = history;
        this.postings = postings;
        this.employers = employers;
    }

    /** A caller who is neither the student who applied nor the posting's employer gets 404, like a missing ID. */
    @Transactional(readOnly = true)
    public ApplicationDetailResponse get(CurrentUser user, Long applicationId) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Posting posting = postings.findById(application.getPostingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        boolean employer = user.getRole() == Role.EMPLOYER;
        Long ownerId = employer ? posting.getEmployerId() : application.getStudentId();
        if (!user.getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return ApplicationDetailResponse.from(application, posting.getTitle(),
                history.findByApplicationIdOrderByCreatedAtAscIdAsc(applicationId),
                employer ? null : contactOf(posting.getEmployerId()));
    }

    private EmployerContact contactOf(Long employerId) {
        EmployerProfileResponse profile = employers.get(employerId);
        return new EmployerContact(profile.getDisplayName(), profile.getContactEmail(), profile.getPhone());
    }
}
