package com.workmates.profile.dto;

import com.workmates.profile.entity.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A skill or language tag")
public class TagResponse {
    @Schema(description = "Tag ID", example = "7")
    private Long id;
    @Schema(description = "Lowercase tag name", example = "java")
    private String name;

    public static TagResponse from(Tag tag) {
        TagResponse response = new TagResponse();
        response.id = tag.getId();
        response.name = tag.getName();
        return response;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
