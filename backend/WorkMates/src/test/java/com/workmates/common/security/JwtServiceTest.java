package com.workmates.common.security;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofDays(7));

    @Test
    void issuedTokenParsesBackToTheSameUser() {
        String token = jwtService.issue(42L, Role.EMPLOYER);

        CurrentUser user = jwtService.parse(token);

        assertEquals(new CurrentUser(42L, Role.EMPLOYER), user);
    }

    @Test
    void expiredTokenIsUnauthorized() {
        String token = new JwtService(SECRET, Duration.ofSeconds(-10)).issue(1L, Role.STUDENT);

        assertUnauthorized(token);
    }

    @Test
    void tokenSignedWithAnotherSecretIsUnauthorized() {
        String token = new JwtService(OTHER_SECRET, Duration.ofDays(7)).issue(1L, Role.STUDENT);

        assertUnauthorized(token);
    }

    @Test
    void garbageTokenIsUnauthorized() {
        assertUnauthorized("not-a-token");
    }

    private void assertUnauthorized(String token) {
        BusinessException e = assertThrows(BusinessException.class, () -> jwtService.parse(token));
        assertEquals(ErrorCode.UNAUTHORIZED, e.getCode());
    }
}
