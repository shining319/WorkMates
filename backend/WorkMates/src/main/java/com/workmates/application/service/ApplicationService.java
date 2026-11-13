package com.workmates.application.service;

import com.workmates.application.dto.ApplicationResponse;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.service.NotificationMessages;
import com.workmates.notification.service.NotificationService;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.service.PostingService;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.service.StudentProfileService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Student applications. Applying is the only write path here for now. */
@Service
public class ApplicationService {
    private final ApplicationRepository applications;
    private final ApplicationStateMachine stateMachine;
    private final PostingService postings;
    private final StudentProfileService profiles;
    private final NotificationService notifications;

    public ApplicationService(ApplicationRepository applications, ApplicationStateMachine stateMachine,
                              PostingService postings, StudentProfileService profiles,
                              NotificationService notifications) {
        this.applications = applications;
        this.stateMachine = stateMachine;
        this.postings = postings;
        this.profiles = profiles;
        this.notifications = notifications;
    }

    /**
     * Applies to a posting with the student's saved profile (architecture section 8.3). The posting is locked
     * first. Then: the posting must be open, the profile complete and no earlier application may exist.
     * The employer is told about the new application.
     */
    @Transactional
    public ApplicationResponse apply(Long studentId, Long postingId) {
        Posting posting = postings.lockPosting(postingId);
        if (posting.getStatus() != PostingStatus.OPEN) {
            throw new BusinessException(ErrorCode.POSTING_CLOSED);
        }
        StudentProfileResponse profile = profiles.get(studentId);
        if (!profile.isComplete()) {
            throw new BusinessException(ErrorCode.PROFILE_INCOMPLETE,
                    ErrorCode.PROFILE_INCOMPLETE.getDefaultMessage() + " (missing: "
                            + String.join(", ", profile.getMissing()) + ")");
        }
        if (applications.existsByPostingIdAndStudentId(postingId, studentId)) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        Application application = new Application();
        application.setPostingId(postingId);
        application.setStudentId(studentId);
        application.setStatus(ApplicationStatus.APPLIED);
        try {
            // Flush now, so a clash with the unique (posting, student) index shows up here
            applications.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }
        stateMachine.recordApplied(application, studentId);
        notifications.notify(posting.getEmployerId(), NotificationType.APPLICATION_RECEIVED,
                NotificationMessages.applicationReceived(profile.getName(), posting.getTitle()),
                NotificationTargetType.APPLICATION, application.getId());
        return ApplicationResponse.from(application);
    }
}
