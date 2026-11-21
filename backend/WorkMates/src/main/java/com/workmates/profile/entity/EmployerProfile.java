package com.workmates.profile.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
@Entity
@Table(name="employer_profiles")
public class EmployerProfile {
    @Id @Column(name="user_id")
    private Long userId;
    @Column(name="display_name",nullable=false,length=100)
    private String displayName;
    @Column(length=1000)
    private String bio;
    @Column(name="contact_email",nullable=false,length=254)
    private String contactEmail;
    @Column(length=30)
    private String phone;
    @UpdateTimestamp @Column(name="updated_at",nullable=false)
    private Instant updatedAt;
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { this.displayName = value; }
    public String getBio() { return bio; }
    public void setBio(String value) { this.bio = value; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String value) { this.contactEmail = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
