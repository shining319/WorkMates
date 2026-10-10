package com.workmates.common.security;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthInterceptorTest {

    private final JwtService jwtService = new JwtService(
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes()), Duration.ofDays(7));
    private final AuthInterceptor interceptor = new AuthInterceptor(jwtService);

    private MockHttpServletRequest request;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        assertCode(ErrorCode.UNAUTHORIZED, handler("anyUser"));
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        request.addHeader("Authorization", "Bearer garbage");

        assertCode(ErrorCode.UNAUTHORIZED, handler("anyUser"));
    }

    @Test
    void validTokenStoresCurrentUser() throws Exception {
        loginAs(7L, Role.STUDENT);

        assertTrue(interceptor.preHandle(request, response, handler("anyUser")));
        assertEquals(new CurrentUser(7L, Role.STUDENT), request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR));
    }

    @Test
    void methodRoleMismatchIsAccessDenied() throws Exception {
        loginAs(7L, Role.STUDENT);

        assertCode(ErrorCode.ACCESS_DENIED, handler("employerOnly"));
    }

    @Test
    void methodRoleMatchPasses() throws Exception {
        loginAs(7L, Role.EMPLOYER);

        assertTrue(interceptor.preHandle(request, response, handler("employerOnly")));
    }

    @Test
    void classRoleAppliesWhenMethodHasNone() throws Exception {
        loginAs(7L, Role.EMPLOYER);

        assertCode(ErrorCode.ACCESS_DENIED, new HandlerMethod(new StudentController(), StudentController.class.getMethod("list")));
    }

    private void loginAs(Long id, Role role) {
        request.addHeader("Authorization", "Bearer " + jwtService.issue(id, role));
    }

    private void assertCode(ErrorCode expected, HandlerMethod handler) {
        BusinessException e = assertThrows(BusinessException.class, () -> interceptor.preHandle(request, response, handler));
        assertEquals(expected, e.getCode());
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new DummyController(), DummyController.class.getMethod(methodName));
    }

    static class DummyController {
        public void anyUser() {
        }

        @RequireRole(Role.EMPLOYER)
        public void employerOnly() {
        }
    }

    @RequireRole(Role.STUDENT)
    static class StudentController {
        public void list() {
        }
    }
}
