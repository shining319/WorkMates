package com.workmates.profile.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
@Entity
@Table(name="student_profiles")
public class StudentProfile {
    @Id @Column(name="user_id")
    private Long userId;
    @Column(length=30)
    private String phone;
    @Column(length=100)
    private String major;
    @Column(name="year_of_study",length=30)
    private String yearOfStudy;
    @Column(name="expected_hourly_rate",precision=8,scale=2)
    private BigDecimal expectedHourlyRate;
    @Column(name="match_alerts_enabled",nullable=false)
    private Boolean matchAlertsEnabled;
    @UpdateTimestamp @Column(name="updated_at",nullable=false)
    private Instant updatedAt;
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone = value; }
    public String getMajor() { return major; }
    public void setMajor(String value) { this.major = value; }
    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String value) { this.yearOfStudy = value; }
    public BigDecimal getExpectedHourlyRate() { return expectedHourlyRate; }
    public void setExpectedHourlyRate(BigDecimal value) { this.expectedHourlyRate = value; }
    public Boolean getMatchAlertsEnabled() { return matchAlertsEnabled; }
    public void setMatchAlertsEnabled(Boolean value) { this.matchAlertsEnabled = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
