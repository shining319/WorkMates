package com.workmates.account.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false, length = 254)
	private String email;
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;
	@Column(nullable = false, length = 100)
	private String name;
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long value) {
		this.id = value;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String value) {
		this.email = value;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String value) {
		this.passwordHash = value;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role value) {
		this.role = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant value) {
		this.createdAt = value;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant value) {
		this.updatedAt = value;
	}
}
