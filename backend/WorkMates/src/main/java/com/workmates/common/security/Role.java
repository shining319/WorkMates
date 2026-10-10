package com.workmates.common.security;

/**
 * The two kinds of account. The role is chosen at registration and never changes.
 * It is stored in the token as the {@code role} claim and checked by {@link RequireRole}.
 */
public enum Role {
    /** A TUS student looking for a job. Registers with a @student.tus.ie email. */
    STUDENT,
    /** An employer who posts jobs. Registers with any other valid email. */
    EMPLOYER
}
