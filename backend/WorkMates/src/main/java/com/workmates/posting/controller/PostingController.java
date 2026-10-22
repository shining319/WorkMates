package com.workmates.posting.controller;

import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
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
import org.springframework.web.bind.annotation.PostMapping;
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
}
