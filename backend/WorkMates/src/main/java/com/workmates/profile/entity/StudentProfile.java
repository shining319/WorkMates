package com.workmates.profile.entity;

import com.workmates.common.timeslot.TimeSlot;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String major;

    @Column(name = "year_of_study", length = 30)
    private String yearOfStudy;

    @Column(name = "expected_hourly_rate", precision = 8, scale = 2)
    private BigDecimal expectedHourlyRate;

    @Column(name = "match_alerts_enabled", nullable = false)
    private Boolean matchAlertsEnabled;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany
    @JoinTable(name = "student_tags", joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "student_time_slots", joinColumns = @JoinColumn(name = "student_id"))
    private List<TimeSlot> timeSlots = new ArrayList<>();
}
