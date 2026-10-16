package com.workmates.account.service;

import com.workmates.account.dto.*;
import com.workmates.account.entity.*;
import com.workmates.account.repository.*;
import com.workmates.common.*;
import com.workmates.common.security.*;
import com.workmates.profile.service.ProfileRegistrationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AccountService {
	private final UserRepository users;
	private final JwtService jwt;
	private final BCryptPasswordEncoder encoder;
	private final ProfileRegistrationService profiles;

	public AccountService(UserRepository users, JwtService jwt, BCryptPasswordEncoder encoder,
			ProfileRegistrationService profiles) {
		this.users = users;
		this.jwt = jwt;
		this.encoder = encoder;
		this.profiles = profiles;
	}

	private String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public UserResponse toResponse(User user) {
		UserResponse r = new UserResponse();
		r.setId(user.getId());
		r.setName(user.getName());
		r.setEmail(user.getEmail());
		r.setRole(user.getRole());
		return r;
	}

	@Transactional
	public AuthResponse register(RegisterRequest req) {
		String email = normalize(req.getEmail());
		boolean student = email.endsWith("@student.tus.ie");
		if (req.getRole() == Role.STUDENT && !student)
			throw new BusinessException(ErrorCode.INVALID_EMAIL_DOMAIN);
		if (req.getRole() == Role.EMPLOYER && student)
			throw new BusinessException(ErrorCode.STUDENT_EMAIL_FOR_EMPLOYER);
		if (!req.getPassword().equals(req.getConfirmPassword()))
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Passwords do not match");
		if (users.existsByEmailIgnoreCase(email))
			throw new BusinessException(ErrorCode.EMAIL_TAKEN);
		User user = new User();
		user.setName(req.getName().trim());
		user.setEmail(email);
		user.setPasswordHash(encoder.encode(req.getPassword()));
		user.setRole(req.getRole());
		// Save first, then create an empty matching profile within the same
		// transaction.
		// Unique index remains the final protection against concurrent duplicate
		// emails.
		try {
			user = users.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(ErrorCode.EMAIL_TAKEN);
		}
		profiles.createEmpty(user.getId(), user.getRole(), user.getName(), user.getEmail());
		return auth(user);
	}

	public AuthResponse login(LoginRequest req) {
		User u = users.findByEmailIgnoreCase(normalize(req.getEmail())).orElse(null);
		if (u == null || !encoder.matches(req.getPassword(), u.getPasswordHash()))
			throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
		return auth(u);
	}

	public UserResponse me(CurrentUser current) {
		return toResponse(getUser(current.getId()));
	}

	public User getUser(Long id) {
		return users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	@Transactional
	public void changeName(Long id, String name) {
		User u = getUser(id);
		u.setName(name.trim());
	}

	private AuthResponse auth(User u) {
		AuthResponse response = new AuthResponse();
		response.setToken(jwt.issue(u.getId(), u.getRole()));
		response.setUser(toResponse(u));
		return response;
	}
}
