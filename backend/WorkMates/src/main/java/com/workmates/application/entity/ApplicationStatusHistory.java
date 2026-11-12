package com.workmates.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** One status change of an application. Only {@code ApplicationStateMachine} writes these. */
@Entity
@Table(name = "application_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /** Empty in the first row, when the application was created. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private ActorType actorType;

    /** The actor's user ID. Empty when the system made the change. */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ApplicationStatusReason reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ApplicationStatusHistory(Long applicationId, ApplicationStatus fromStatus, ApplicationStatus toStatus,
                                    ActorType actorType, Long actorUserId, ApplicationStatusReason reason) {
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorType = actorType;
        this.actorUserId = actorUserId;
        this.reason = reason;
    }
}
