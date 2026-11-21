package com.workmates.profile.controller;

import com.workmates.common.Result;
import com.workmates.profile.dto.TagResponse;
import com.workmates.profile.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags", description = "Skill and language tag suggestions")
public class TagController {

    /** Schema-only class for Swagger: shows the real type of {@code data}. Never created with {@code new}. */
    @Schema(description = "Tag suggestions response")
    private static class TagListResult extends Result<List<TagResponse>> {
        @Schema(description = "Up to 10 matching tags")
        @Override
        public List<TagResponse> getData() {
            return super.getData();
        }
    }

    private final TagService service;

    public TagController(TagService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Suggest existing tags whose name starts with the typed text")
    @ApiResponse(responseCode = "200", description = "At most 10 tags, ordered by name",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TagListResult.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Result.class)))
    public Result<List<TagResponse>> suggest(@RequestParam(required = false, defaultValue = "") String q) {
        return Result.ok(service.suggest(q).stream().map(TagResponse::from).toList());
    }
}
