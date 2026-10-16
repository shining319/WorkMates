package com.workmates.profile.service;

import com.workmates.common.security.Role;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.entity.StudentProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import com.workmates.profile.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;

/** Creates the mandatory empty role-specific profile during account registration. */
@Service
public class ProfileRegistrationService {
    private final StudentProfileRepository students;
    private final EmployerProfileRepository employers;

    public ProfileRegistrationService(StudentProfileRepository students,
                                      EmployerProfileRepository employers) {
        this.students = students;
        this.employers = employers;
    }

    public void createEmpty(Long userId, Role role, String name, String email) {
        if (role == Role.STUDENT) {
            StudentProfile profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setMatchAlertsEnabled(true);
            students.save(profile);
        } else if (role == Role.EMPLOYER) {
            EmployerProfile profile = new EmployerProfile();
            profile.setUserId(userId);
            profile.setDisplayName(name);
            profile.setContactEmail(email);
            employers.save(profile);
        } else {
            throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }
}
