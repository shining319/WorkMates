package com.workmates.posting.entity;

import com.workmates.common.timeslot.TimeSlot;
import com.workmates.profile.entity.Tag;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A job posting created by an employer. */
@Entity
@Table(name = "postings")
@Getter
@Setter
public class Posting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Column(name = "hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "weekly_hours", nullable = false, precision = 4, scale = 1)
    private BigDecimal weeklyHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Area area;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Integer headcount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason", length = 10)
    private CloseReason closeReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(name = "posting_tags", joinColumns = @JoinColumn(name = "posting_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @ElementCollection
    @BatchSize(size = 100) // search reads the slots of many postings: load them with IN (...) instead of one query each
    @CollectionTable(name = "posting_time_slots", joinColumns = @JoinColumn(name = "posting_id"))
    private List<TimeSlot> timeSlots = new ArrayList<>();
}
