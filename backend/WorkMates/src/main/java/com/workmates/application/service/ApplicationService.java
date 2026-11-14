package com.workmates.application.service;

import com.workmates.application.dto.ApplicationResponse;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.Role;
import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.service.NotificationMessages;
import com.workmates.notification.service.NotificationService;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import com.workmates.posting.service.PostingService;
import com.workmates.profile.dto.StudentProfileResponse;
import com.workmates.profile.service.StudentProfileService;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Student applications: applying, and the employer's and student's status changes. */
@Service
public class ApplicationService {
    private static final List<ApplicationStatus> HIRED = List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED);

    private final ApplicationRepository applications;
    private final ApplicationStateMachine stateMachine;
    private final PostingService postings;
    private final StudentProfileService profiles;
    private final NotificationService notifications;
    private final HeadcountService headcount;

    public ApplicationService(ApplicationRepository applications, ApplicationStateMachine stateMachine,
                              PostingService postings, StudentProfileService profiles,
                              NotificationService notifications, HeadcountService headcount) {
        this.applications = applications;
        this.stateMachine = stateMachine;
        this.postings = postings;
        this.profiles = profiles;
        this.notifications = notifications;
        this.headcount = headcount;
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

    /**
     * A manual status change (architecture section 8.3). Reads the posting ID, locks the posting, then reads the
     * application again so it sees what other transactions committed. A caller who does not own the posting
     * (employer) or the application (student) gets 404. {@code COMPLETED} is not open yet (US 4.7).
     */
    @Transactional
    public ApplicationResponse changeStatus(CurrentUser user, Long applicationId, ApplicationStatus to) {
        Long postingId = applications.findPostingIdById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Posting posting = postings.lockPosting(postingId);
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        boolean employer = user.getRole() == Role.EMPLOYER;
        Long ownerId = employer ? posting.getEmployerId() : application.getStudentId();
        if (!user.getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (to == ApplicationStatus.COMPLETED) {
            throw invalidTransition(application);
        }
        if (employer) {
            advanceByEmployer(posting, application, to, user.getId());
        } else {
            answerOffer(posting, application, to, user.getId());
        }
        return ApplicationResponse.from(application);
    }

    private void advanceByEmployer(Posting posting, Application application, ApplicationStatus to, Long employerId) {
        stateMachine.transition(application, to, ActorType.EMPLOYER, employerId, null);
        notifications.notify(application.getStudentId(), NotificationType.STATUS_CHANGED,
                NotificationMessages.statusChanged(posting.getTitle(), to.name()),
                NotificationTargetType.APPLICATION, application.getId());
    }

    private void answerOffer(Posting posting, Application application, ApplicationStatus to, Long studentId) {
        if (application.getStatus() == ApplicationStatus.REJECTED
                && application.getStatusReason() == ApplicationStatusReason.POSITION_FILLED) {
            throw new BusinessException(ErrorCode.POSITION_FILLED);
        }
        boolean accept = to == ApplicationStatus.ACCEPTED;
        if (accept && application.getStatus() == ApplicationStatus.OFFER
                && applications.countByPostingIdAndStatusIn(posting.getId(), HIRED) >= posting.getHeadcount()) {
            throw new BusinessException(ErrorCode.POSITION_FILLED);
        }
        // The state machine refuses anything but ACCEPTED or DECLINED from OFFER
        stateMachine.transition(application, to, ActorType.STUDENT, studentId, null);
        String name = profiles.get(studentId).getName();
        notifications.notify(posting.getEmployerId(),
                accept ? NotificationType.OFFER_ACCEPTED : NotificationType.OFFER_DECLINED,
                accept ? NotificationMessages.offerAccepted(name, posting.getTitle())
                        : NotificationMessages.offerDeclined(name, posting.getTitle()),
                NotificationTargetType.APPLICATION, application.getId());
        if (accept) {
            headcount.closeIfFilled(posting);
        }
    }

    private static BusinessException invalidTransition(Application application) {
        return new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                ErrorCode.INVALID_STATUS_TRANSITION.getDefaultMessage() + " Current status: " + application.getStatus());
    }
}
