package com.workmates.common;

/**
 * Thrown by services (and the auth interceptor) when a business rule is broken.
 * GlobalExceptionHandler turns it into a {@link Result} with the HTTP status of its {@link ErrorCode}.
 *
 * <p>It is unchecked, so a throw inside a {@code @Transactional} method rolls the transaction back.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    /** Uses the code's default message. */
    public BusinessException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    /** Use when the message needs values filled in, for example the current count. */
    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
