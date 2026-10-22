package com.workmates.posting.controller;

import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
import com.workmates.posting.dto.PostingDetailResponse;
import com.workmates.posting.dto.PostingRequest;
import com.workmates.posting.dto.PostingResponse;
import com.workmates.posting.service.PostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/postings")
@Tag(name = "Postings", description = "Job postings")
public class PostingController {

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Posting response")
    private static class PostingResult extends Result<PostingResponse> {
        @Schema(description = "The posting")
        @Override
        public PostingResponse getData() {
            return super.getData();
        }
    }

    /** Schema-only class for Swagger, like {@link PostingResult}. */
    @Schema(description = "Posting detail response")
    private static class PostingDetailResult extends Result<PostingDetailResponse> {
        @Schema(description = "The posting with its employer and application counts")
        @Override
        public PostingDetailResponse getData() {
            return super.getData();
        }
    }

    private final PostingService service;

    public PostingController(PostingService service) { this.service = service; }

    @PostMapping
    @RequireRole(Role.EMPLOYER)
    @Operation(summary = "Create a posting. It is saved as OPEN and appears in student search.")
    @ApiResponse(responseCode = "201", description = "The new posting",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostingResult.class)))
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_FAILED / TIME_SLOT_INVALID / WEEKLY_HOURS_EXCEED_SLOTS",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not an employer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<PostingResponse>> create(CurrentUser user, @Valid @RequestBody PostingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(service.create(user.getId(), request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Posting detail with the employer's name and bio and the application counts. Closed postings too.")
    @ApiResponse(responseCode = "200", description = "The posting",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostingDetailResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<PostingDetailResponse> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequireRole(Role.EMPLOYER)
    @Operation(summary = "Edit one of my open postings. Tags and time slots are replaced as a whole.")
    @ApiResponse(responseCode = "200", description = "The updated posting",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostingResult.class)))
    @ApiResponse(responseCode = "400",
            description = "VALIDATION_FAILED / TIME_SLOT_INVALID / WEEKLY_HOURS_EXCEED_SLOTS",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not an employer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND (missing, or another employer's posting)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "409",
            description = "POSTING_NOT_EDITABLE (closed) / HEADCOUNT_BELOW_FILLED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<PostingResponse> update(CurrentUser user, @PathVariable Long id,
                                          @Valid @RequestBody PostingRequest request) {
        return Result.ok(service.update(user.getId(), id, request));
    }

    @PostMapping("/{id}/close")
    @RequireRole(Role.EMPLOYER)
    @Operation(summary = "Close one of my open postings by hand. Applications in progress keep their status.")
    @ApiResponse(responseCode = "200", description = "The closed posting (CLOSED, MANUAL)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostingResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not an employer)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND (missing, or another employer's posting)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "409", description = "POSTING_NOT_EDITABLE (already closed)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<PostingResponse> close(CurrentUser user, @PathVariable Long id) {
        return Result.ok(service.close(user.getId(), id));
    }
}
