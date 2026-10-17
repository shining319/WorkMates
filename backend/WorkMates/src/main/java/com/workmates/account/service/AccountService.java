package com.workmates.account.service;

import com.workmates.account.dto.AuthResponse;
import com.workmates.account.dto.LoginRequest;
import com.workmates.account.dto.RegisterRequest;
import com.workmates.account.dto.UserResponse;
import com.workmates.account.entity.User;
import com.workmates.account.repository.UserRepository;
import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.JwtService;
import com.workmates.common.security.Role;
import com.workmates.profile.service.ProfileRegistrationService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account authentication. Uses the team's existing shared JWT service and Role enum. */
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
        UserResponse result = new UserResponse();
        result.setId(user.getId());
        result.setName(user.getName());
        result.setEmail(user.getEmail());
        result.setRole(user.getRole());
        return result;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.getEmail());
        boolean studentEmail = email.endsWith("@student.tus.ie");
        if (request.getRole() == Role.STUDENT && !studentEmail) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
        if (request.getRole() == Role.EMPLOYER && studentEmail) {
            throw new BusinessException(ErrorCode.STUDENT_EMAIL_FOR_EMPLOYER);
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Passwords do not match");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException duplicate) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN);
        }
        // Must succeed within the same transaction as account creation.
        profiles.createEmpty(user.getId(), user.getRole(), user.getName(), user.getEmail());
        return auth(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalize(request.getEmail())).orElse(null);
        if (user == null || !encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }
        return auth(user);
    }

    public UserResponse me(CurrentUser caller) {
        return toResponse(getUser(caller.getId()));
    }

    public User getUser(Long id) {
        return users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Transactional
    public void changeName(Long id, String name) {
        getUser(id).setName(name.trim());
    }

    private AuthResponse auth(User user) {
        AuthResponse response = new AuthResponse();
        response.setToken(jwt.issue(user.getId(), user.getRole()));
        response.setUser(toResponse(user));
        return response;
    }
}
