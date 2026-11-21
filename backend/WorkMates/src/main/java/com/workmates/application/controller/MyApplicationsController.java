package com.workmates.application.controller;

import com.workmates.application.dto.MyApplicationResponse;
import com.workmates.application.entity.ApplicationGroup;
import com.workmates.application.service.MyApplicationsService;
import com.workmates.common.PageInfo;
import com.workmates.common.Result;
import com.workmates.common.security.CurrentUser;
import com.workmates.common.security.RequireRole;
import com.workmates.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/mine")
@Tag(name = "Applications", description = "Student applications to postings")
public class MyApplicationsController {
    private final MyApplicationsService service;

    public MyApplicationsController(MyApplicationsService service) {
        this.service = service;
    }

    @GetMapping
    @RequireRole(Role.STUDENT)
    @Operation(summary = "My applications, most recently updated first, 20 per page. Optionally one status group.")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED (group invalid, page below 0)")
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not a student)")
    public Result<List<MyApplicationResponse>> mine(
            CurrentUser user,
            @Parameter(description = "IN_PROGRESS, OFFER, ACCEPTED or FINISHED. Omit for all")
            @RequestParam(required = false) ApplicationGroup group,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page) {
        Page<MyApplicationResponse> found = service.mine(user.getId(), group, page);
        return Result.ok(found.getContent(),
                new PageInfo(found.getNumber(), found.getSize(), found.getTotalElements(), found.getTotalPages()));
    }
}
