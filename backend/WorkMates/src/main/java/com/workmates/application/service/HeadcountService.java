package com.workmates.application.service;

import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationRepository;
import com.workmates.notification.entity.NotificationTargetType;
import com.workmates.notification.entity.NotificationType;
import com.workmates.notification.service.NotificationMessages;
import com.workmates.notification.service.NotificationService;
import com.workmates.posting.entity.CloseReason;
import com.workmates.posting.entity.Posting;
import com.workmates.posting.entity.PostingStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Closes a posting when its headcount is reached (architecture section 8.3). */
@Service
public class HeadcountService {
    private static final List<ApplicationStatus> HIRED = List.of(ApplicationStatus.ACCEPTED, ApplicationStatus.COMPLETED);
    private static final List<ApplicationStatus> IN_PROGRESS = List.of(ApplicationStatus.APPLIED,
            ApplicationStatus.REVIEWED, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER);

    private final ApplicationRepository applications;
    private final ApplicationStateMachine stateMachine;
    private final NotificationService notifications;

    public HeadcountService(ApplicationRepository applications, ApplicationStateMachine stateMachine,
                            NotificationService notifications) {
        this.applications = applications;
        this.stateMachine = stateMachine;
        this.notifications = notifications;
    }

    /**
     * Runs in the caller's transaction, after the caller locked the posting. When Accepted + Completed equals the
     * headcount: closes the posting as FILLED, rejects every application still in progress (by the system) and
     * notifies each of those students and the employer.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void closeIfFilled(Posting posting) {
        if (posting.getStatus() != PostingStatus.OPEN) {
            return;
        }
        long hired = applications.countByPostingIdAndStatusIn(posting.getId(), HIRED);
        if (hired != posting.getHeadcount()) {
            return;
        }
        posting.setStatus(PostingStatus.CLOSED);
        posting.setCloseReason(CloseReason.FILLED);
        posting.setClosedAt(Instant.now());
        for (Application application : applications.findByPostingIdAndStatusIn(posting.getId(), IN_PROGRESS)) {
            stateMachine.transition(application, ApplicationStatus.REJECTED, ActorType.SYSTEM, null,
                    ApplicationStatusReason.POSITION_FILLED);
            notifications.notify(application.getStudentId(), NotificationType.POSITION_FILLED,
                    NotificationMessages.positionFilled(posting.getTitle()),
                    NotificationTargetType.APPLICATION, application.getId());
        }
        notifications.notify(posting.getEmployerId(), NotificationType.POSTING_FILLED,
                NotificationMessages.postingFilled(posting.getTitle()), NotificationTargetType.POSTING, posting.getId());
    }
}
