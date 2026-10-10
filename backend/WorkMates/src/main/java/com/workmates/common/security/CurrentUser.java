package com.workmates.common.security;

import java.util.Objects;

/**
 * The logged-in user, read from the token by {@link AuthInterceptor}.
 * Add it as a controller method parameter to receive it.
 *
 * <p>It holds only what the token carries, so reading it needs no database lookup.
 */
public class CurrentUser {

    /** The user ID (the token's {@code sub} claim). */
    private final Long id;

    /** The user's role. It never changes after registration, so the role in the token can be trusted. */
    private final Role role;

    public CurrentUser(Long id, Role role) {
        this.id = id;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CurrentUser other)) {
            return false;
        }
        return Objects.equals(id, other.id) && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role);
    }

    @Override
    public String toString() {
        return "CurrentUser{id=" + id + ", role=" + role + '}';
    }
}
