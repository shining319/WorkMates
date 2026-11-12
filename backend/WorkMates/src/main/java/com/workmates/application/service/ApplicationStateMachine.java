package com.workmates.application.service;

import static com.workmates.application.entity.ActorType.EMPLOYER;
import static com.workmates.application.entity.ActorType.STUDENT;
import static com.workmates.application.entity.ActorType.SYSTEM;
import static com.workmates.application.entity.ApplicationStatus.ACCEPTED;
import static com.workmates.application.entity.ApplicationStatus.APPLIED;
import static com.workmates.application.entity.ApplicationStatus.COMPLETED;
import static com.workmates.application.entity.ApplicationStatus.DECLINED;
import static com.workmates.application.entity.ApplicationStatus.INTERVIEW;
import static com.workmates.application.entity.ApplicationStatus.OFFER;
import static com.workmates.application.entity.ApplicationStatus.REJECTED;
import static com.workmates.application.entity.ApplicationStatus.REVIEWED;

import com.workmates.application.dto.StatusChange;
import com.workmates.application.entity.ActorType;
import com.workmates.application.entity.Application;
import com.workmates.application.entity.ApplicationStatus;
import com.workmates.application.entity.ApplicationStatusHistory;
import com.workmates.application.entity.ApplicationStatusReason;
import com.workmates.application.repository.ApplicationStatusHistoryRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only code that changes an application's status (architecture section 8.2).
 *
 * <p>It does not check ownership. The calling service checks that first, for example that the employer owns
 * the posting or that the student owns the application. It also does not lock the posting: the caller already did.
 */
@Service
public class ApplicationStateMachine {

    private record Transition(ApplicationStatus from, ApplicationStatus to) {
    }

    /** Each allowed change and who may make it. A change that is not in the table is not allowed. */
    private static final Map<Transition, Set<ActorType>> ALLOWED = Map.ofEntries(
            Map.entry(new Transition(APPLIED, REVIEWED), Set.of(EMPLOYER)),
            Map.entry(new Transition(REVIEWED, INTERVIEW), Set.of(EMPLOYER)),
            Map.entry(new Transition(INTERVIEW, OFFER), Set.of(EMPLOYER)),
            Map.entry(new Transition(APPLIED, REJECTED), Set.of(EMPLOYER, SYSTEM)),
            Map.entry(new Transition(REVIEWED, REJECTED), Set.of(EMPLOYER, SYSTEM)),
            Map.entry(new Transition(INTERVIEW, REJECTED), Set.of(EMPLOYER, SYSTEM)),
            // An employer can not reject or withdraw an offer. Only the system does, when the posting fills.
            Map.entry(new Transition(OFFER, REJECTED), Set.of(SYSTEM)),
            Map.entry(new Transition(OFFER, ACCEPTED), Set.of(STUDENT)),
            Map.entry(new Transition(OFFER, DECLINED), Set.of(STUDENT)),
            Map.entry(new Transition(ACCEPTED, COMPLETED), Set.of(STUDENT, EMPLOYER)));

    private final ApplicationStatusHistoryRepository history;

    public ApplicationStateMachine(ApplicationStatusHistoryRepository history) {
        this.history = history;
    }

    /**
     * Moves the application to {@code to}: checks the table, sets the status, the reason and {@code updatedAt},
     * and adds a history row. {@code reason} is empty when a person made the change. For {@code SYSTEM} the
     * history keeps no user ID.
     *
     * @throws BusinessException {@code INVALID_STATUS_TRANSITION} when this actor may not make this change
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StatusChange transition(Application application, ApplicationStatus to, ActorType actor, Long actorUserId,
                                   ApplicationStatusReason reason) {
        ApplicationStatus from = application.getStatus();
        if (!ALLOWED.getOrDefault(new Transition(from, to), Set.of()).contains(actor)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    ErrorCode.INVALID_STATUS_TRANSITION.getDefaultMessage() + " Current status: " + from);
        }
        application.setStatus(to);
        application.setStatusReason(reason);
        application.setUpdatedAt(Instant.now());
        history.save(new ApplicationStatusHistory(application.getId(), from, to, actor,
                actor == SYSTEM ? null : actorUserId, reason));
        return new StatusChange(application.getId(), from, to, actor);
    }

    /** Writes the first history row, {@code NULL -> APPLIED}, for an application the student just created. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordApplied(Application application, Long studentId) {
        history.save(new ApplicationStatusHistory(application.getId(), null, APPLIED, STUDENT, studentId, null));
    }
}
