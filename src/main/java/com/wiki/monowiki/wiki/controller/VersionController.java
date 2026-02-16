package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.VersionDtos.CreateVersionRequest;
import com.wiki.monowiki.wiki.dto.VersionDtos.VersionResponse;
import com.wiki.monowiki.wiki.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Versions", description = "Article versions. New versions can only be added while the article is DRAFT.")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
	this.versionService = versionService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/versions")
    @Operation(summary = "Add a new version (ADMIN/EDITOR)")
    public BaseResponse<VersionResponse> create(@PathVariable Long id,
	    @Valid @RequestBody CreateVersionRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Version added", false, versionService.create(id, req));
    }

    @GetMapping("/articles/{id}/versions")
    @Operation(summary = "List versions (VIEWER only if article is PUBLISHED)")
    public BasePageResponse<VersionResponse> list(
	    @PathVariable Long id,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "20") int size,
	    @RequestParam(defaultValue = "versionNo,asc") String sort
    ) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(versionService.list(id, pageable), "Versions fetched");
    }

    @GetMapping("/articles/{id}/versions/{no}")
    @Operation(summary = "Get a version (VIEWER only if article is PUBLISHED)")
    public BaseResponse<VersionResponse> get(@PathVariable Long id, @PathVariable Integer no) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Version fetched", false, versionService.get(id, no));
    }

    private Sort parseSort(String sort) {
	try {
	    String[] parts = sort.split(",");
	    String field = parts[0].trim();
	    Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
		    ? Sort.Direction.DESC : Sort.Direction.ASC;
	    return Sort.by(dir, field);
	} catch (Exception e) {
	    return Sort.by(Sort.Direction.ASC, "versionNo");
	}
    }
}
