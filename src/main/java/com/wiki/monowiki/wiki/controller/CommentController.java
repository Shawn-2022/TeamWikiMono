package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.CommentDtos.*;
import com.wiki.monowiki.wiki.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Comments", description = "Comments are attached to a specific article version.")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/versions/{no}/comments")
    @Operation(summary = "Add comment to a version (ADMIN/EDITOR)")
    public BaseResponse<CommentResponse> create(@PathVariable Long id,
	    @PathVariable Integer no,
	    @Valid @RequestBody CreateCommentRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Comment added", false, service.create(id, no, req));
    }

    @GetMapping("/articles/{id}/versions/{no}/comments")
    @Operation(summary = "List comments (VIEWER only if article is PUBLISHED)")
    public BasePageResponse<CommentResponse> list(
	    @PathVariable Long id,
	    @PathVariable Integer no,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "50") int size,
	    @RequestParam(defaultValue = "createdAt,asc") String sort
    ) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(service.list(id, no, pageable), "Comments fetched");
    }

    private Sort parseSort(String sort) {
	try {
	    String[] parts = sort.split(",");
	    String field = parts[0].trim();
	    Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
		    ? Sort.Direction.DESC : Sort.Direction.ASC;
	    return Sort.by(dir, field);
	} catch (Exception e) {
	    return Sort.by(Sort.Direction.ASC, "createdAt");
	}
    }
}
