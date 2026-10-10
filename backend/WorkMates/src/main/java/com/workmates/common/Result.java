package com.workmates.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The unified response body. Every endpoint returns a {@code Result<T>} together with a real HTTP status code.
 *
 * <p>Build it with {@link #ok(Object)}, {@link #ok(List, PageInfo)} or {@link #fail(ErrorCode, String)}.
 * The constructor is private so the fields can never be in an inconsistent state, for example
 * {@code success = true} together with an error code.
 *
 * <p>For Swagger, a controller can declare a schema-only subclass such as
 * {@code private static class PostingResult extends Result<PostingVO> {}} and use it in
 * {@code @Schema(implementation = ...)}, so the documented {@code data} has its real type.
 *
 * @param <T> the type of {@link #getData()}; a {@code List} for paged results
 */
@Schema(description = "Unified response body. Every endpoint returns it, together with a real HTTP status code")
public class Result<T> {

    /** True for a successful call, false for an error. */
    @Schema(description = "True for a successful call, false for an error")
    private final boolean success;

    /** The payload. Null on error. For paged results it is the list of items on the current page. */
    @Schema(description = "The payload; null on error. For a paged list, the items on the current page")
    private final T data;

    /** The {@link ErrorCode} name, for example {@code POSITION_FILLED}. Null on success. */
    @Schema(description = "ErrorCode name, for example POSITION_FILLED; null on success", example = "POSITION_FILLED")
    private final String errorCode;

    /** A message that is safe to show to the user. Null on success. */
    @Schema(description = "Message that is safe to show to the user; null on success")
    private final String errorMsg;

    /** Paging details. Non-null only for paged lists. */
    @Schema(description = "Paging details; present only for paged lists")
    private final PageInfo page;

    /**
     * For schema-only subclasses (see the class comment). Never create a {@code Result} this way:
     * use {@link #ok(Object)}, {@link #ok(List, PageInfo)} or {@link #fail(ErrorCode, String)}.
     */
    protected Result() {
        this(false, null, null, null, null);
    }

    private Result(boolean success, T data, String errorCode, String errorMsg, PageInfo page) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.page = page;
    }

    /** A successful result with a single object (or no data at all). */
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, data, null, null, null);
    }

    /** A successful result for a paged list. {@code items} are the items on this page only. */
    public static <T> Result<List<T>> ok(List<T> items, PageInfo page) {
        return new Result<>(true, items, null, null, page);
    }

    /** A failed result. The caller (usually GlobalExceptionHandler) sets the matching HTTP status. */
    public static Result<Void> fail(ErrorCode code, String msg) {
        return new Result<>(false, null, code.name(), msg, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public PageInfo getPage() {
        return page;
    }
}
