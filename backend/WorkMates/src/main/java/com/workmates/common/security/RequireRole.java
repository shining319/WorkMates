package com.workmates.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a controller class or method to one role. {@link AuthInterceptor} checks it and returns
 * 403 {@code ACCESS_DENIED} when the caller has another role.
 *
 * <p>A method annotation wins over the class annotation. Without this annotation an endpoint is open
 * to any logged-in user. Public endpoints (register, login) are excluded in WebConfig instead.
 * This only checks the role. Ownership checks (for example "this is my posting") belong in services.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /** The only role allowed to call the endpoint. */
    Role value();
}
