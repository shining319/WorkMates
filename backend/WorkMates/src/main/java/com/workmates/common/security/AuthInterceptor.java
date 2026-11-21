package com.workmates.common.security;

import com.workmates.common.BusinessException;
import com.workmates.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authenticates every request to /api/** (the paths are set in WebConfig, which skips register and login).
 *
 * <ol>
 *   <li>Reads the {@code Authorization: Bearer <token>} header and verifies the token.</li>
 *   <li>Stores a {@link CurrentUser} as a request attribute for {@link CurrentUserArgumentResolver}.</li>
 *   <li>Checks {@link RequireRole} and rejects a caller with the wrong role.</li>
 * </ol>
 *
 * <p>Exceptions thrown here go through GlobalExceptionHandler, so the client gets the usual {@code Result}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** The request attribute that holds the {@link CurrentUser}. */
    public static final String CURRENT_USER_ATTR = AuthInterceptor.class.getName() + ".currentUser";

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * @return true to continue with the controller
     * @throws BusinessException UNAUTHORIZED for a missing, invalid or expired token;
     *                           ACCESS_DENIED when the role does not match {@link RequireRole}
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Not a controller method (for example an unknown path): let Spring answer with 404
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        CurrentUser user = jwtService.parse(header.substring(BEARER.length()).trim());
        request.setAttribute(CURRENT_USER_ATTR, user);

        // The method annotation wins over the class annotation
        RequireRole required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRole.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRole.class);
        }
        if (required != null && required.value() != user.getRole()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return true;
    }
}
