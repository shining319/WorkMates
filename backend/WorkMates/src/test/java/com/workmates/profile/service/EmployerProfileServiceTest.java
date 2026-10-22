package com.workmates.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import com.workmates.profile.dto.EmployerProfileRequest;
import com.workmates.profile.dto.EmployerProfileResponse;
import com.workmates.profile.entity.EmployerProfile;
import com.workmates.profile.repository.EmployerProfileRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployerProfileServiceTest {
    private static final Long ID = 5L;
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final EmployerProfileRepository profiles = mock(EmployerProfileRepository.class);
    private final EmployerProfileService service = new EmployerProfileService(profiles);

    private EmployerProfile profile;

    @BeforeEach
    void setUp() {
        // What registration creates: display name from the account name, contact email from the login email
        profile = new EmployerProfile();
        profile.setUserId(ID);
        profile.setDisplayName("Sam Cafe");
        profile.setContactEmail("sam@example.com");
        when(profiles.findById(ID)).thenReturn(Optional.of(profile));
    }

    private static EmployerProfileRequest request() {
        EmployerProfileRequest r = new EmployerProfileRequest();
        r.setDisplayName("Campus Cafe");
        r.setBio("A small cafe.");
        r.setContactEmail("jobs@campuscafe.ie");
        r.setPhone("+353 90 1");
        return r;
    }

    private static Set<String> invalidFields(EmployerProfileRequest r) {
        return VALIDATOR.validate(r).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void readsRegistrationDefaults() {
        EmployerProfileResponse response = service.get(ID);

        assertEquals("Sam Cafe", response.getDisplayName());
        assertEquals("sam@example.com", response.getContactEmail());
        assertNull(response.getBio());
        assertNull(response.getPhone());
    }

    @Test
    void unknownEmployerIsNotFound() {
        when(profiles.findById(99L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.get(99L));
        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
    }

    @Test
    void savesAllFieldsAndReturnsThem() {
        EmployerProfileResponse response = service.update(ID, request());

        assertEquals("Campus Cafe", profile.getDisplayName());
        assertEquals("A small cafe.", profile.getBio());
        assertEquals("jobs@campuscafe.ie", profile.getContactEmail());
        assertEquals("+353 90 1", profile.getPhone());
        assertEquals("Campus Cafe", response.getDisplayName());
        assertEquals("jobs@campuscafe.ie", response.getContactEmail());
    }

    @Test
    void blankOptionalFieldsAreStoredAsNull() {
        service.update(ID, request());
        EmployerProfileRequest r = request();
        r.setBio("  ");
        r.setPhone("");

        service.update(ID, r);

        assertNull(profile.getBio());
        assertNull(profile.getPhone());
    }

    @Test
    void validRequestHasNoViolations() {
        assertTrue(invalidFields(request()).isEmpty());
    }

    @Test
    void displayNameIsRequiredAndAtMost100() {
        EmployerProfileRequest r = request();
        r.setDisplayName("   ");
        assertEquals(Set.of("displayName"), invalidFields(r));

        r.setDisplayName("a".repeat(100));
        assertTrue(invalidFields(r).isEmpty());
        r.setDisplayName("a".repeat(101));
        assertEquals(Set.of("displayName"), invalidFields(r));
    }

    @Test
    void contactEmailIsRequiredAndMustBeAnEmail() {
        EmployerProfileRequest r = request();
        r.setContactEmail("");
        assertEquals(Set.of("contactEmail"), invalidFields(r));

        r.setContactEmail("not-an-email");
        assertEquals(Set.of("contactEmail"), invalidFields(r));

        r.setContactEmail("  jobs@campuscafe.ie  ");
        assertTrue(invalidFields(r).isEmpty());
        assertEquals("jobs@campuscafe.ie", r.getContactEmail());
    }

    @Test
    void contactEmailIsAtMost254() {
        EmployerProfileRequest r = request();
        r.setContactEmail("a".repeat(250) + "@b.ie");
        assertEquals(Set.of("contactEmail"), invalidFields(r));
    }

    @Test
    void bioIsAtMost1000AndPhoneAtMost30() {
        EmployerProfileRequest r = request();
        r.setBio("a".repeat(1000));
        r.setPhone("1".repeat(30));
        assertTrue(invalidFields(r).isEmpty());

        r.setBio("a".repeat(1001));
        r.setPhone("1".repeat(31));
        assertEquals(Set.of("bio", "phone"), invalidFields(r));
    }
}
