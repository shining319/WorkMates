package com.workmates.profile.service;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.profile.dto.EmployerProfileRequest;
import com.workmates.profile.dto.EmployerProfileResponse;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and saves an employer's profile. */
@Service
public class EmployerProfileService {
    private final EmployerProfileRepository profiles;

    public EmployerProfileService(EmployerProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public EmployerProfileResponse get(Long employerId) {
        return toResponse(load(employerId));
    }

    @Transactional
    public EmployerProfileResponse update(Long employerId, EmployerProfileRequest request) {
        EmployerProfile profile = load(employerId);
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(blankToNull(request.getBio()));
        profile.setContactEmail(request.getContactEmail());
        profile.setPhone(blankToNull(request.getPhone()));
        return toResponse(profile);
    }

    private EmployerProfile load(Long employerId) {
        return profiles.findById(employerId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private static EmployerProfileResponse toResponse(EmployerProfile profile) {
        EmployerProfileResponse response = new EmployerProfileResponse();
        response.setDisplayName(profile.getDisplayName());
        response.setBio(profile.getBio());
        response.setContactEmail(profile.getContactEmail());
        response.setPhone(profile.getPhone());
        return response;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
