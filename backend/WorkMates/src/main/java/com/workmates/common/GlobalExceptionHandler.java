package com.workmates.common;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into a {@link Result#fail} with the HTTP status of its {@link ErrorCode}.
 *
 * <p>It also covers exceptions thrown by the auth interceptor, because interceptors run inside the
 * DispatcherServlet. The handler for {@link Exception} is the last resort: the stack trace is logged,
 * but the client only sees {@code INTERNAL_ERROR}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A broken business rule: use the status and message of the exception's code. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return respond(e.getCode(), e.getMessage());
    }

    /** A request body failed its {@code @Valid} checks: report the first invalid field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalidBody(MethodArgumentNotValidException e) {
        var error = e.getBindingResult().getFieldErrors().stream().findFirst();
        return respond(ErrorCode.VALIDATION_FAILED,
                error.map(f -> f.getDefaultMessage()).orElse(ErrorCode.VALIDATION_FAILED.getDefaultMessage()));
    }

    /** A query or path parameter failed its constraint annotation: report the first violation. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        var violation = e.getConstraintViolations().stream().findFirst();
        return respond(ErrorCode.VALIDATION_FAILED,
                violation.map(v -> v.getMessage()).orElse(ErrorCode.VALIDATION_FAILED.getDefaultMessage()));
    }

    /** Unreadable JSON, a missing required parameter or a parameter of the wrong type. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        return respond(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage());
    }

    /** No controller or static file for the path. Without this the catch-all below would return 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDefaultMessage());
    }

    /**
     * Anything unexpected. Log it, but send no details to the client.
     *
     * <p>Every endpoint can fail this way, so this is the only handler declared for Swagger here:
     * springdoc adds a {@code @ResponseStatus} handler's response to all endpoints. Other errors are
     * declared with {@code @ApiResponse} on the endpoints that can return them.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: unexpected server error",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("Unexpected error", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }

    private ResponseEntity<Result<Void>> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(Result.fail(code, message));
    }
}
