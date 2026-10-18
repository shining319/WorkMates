package com.workmates.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * Paging details returned next to a paged list in {@link Result}.
 */
@Schema(description = "Paging details, returned next to a paged list")
public class PageInfo {

    /** The current page number, 0-based. */
    @Schema(description = "Current page number, starting at 0", example = "0")
    private final int page;

    /** The requested page size. */
    @Schema(description = "Requested page size", example = "20")
    private final int size;

    /** The number of items across all pages. */
    @Schema(description = "Number of items across all pages", example = "57")
    private final long totalElements;

    /** The number of pages. */
    @Schema(description = "Number of pages", example = "3")
    private final int totalPages;

    public PageInfo(int page, int size, long totalElements, int totalPages) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageInfo other)) {
            return false;
        }
        return page == other.page && size == other.size
                && totalElements == other.totalElements && totalPages == other.totalPages;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, totalElements, totalPages);
    }

    @Override
    public String toString() {
        return "PageInfo{page=" + page + ", size=" + size
                + ", totalElements=" + totalElements + ", totalPages=" + totalPages + '}';
    }
}
