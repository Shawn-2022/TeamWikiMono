package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.VersionDtos.*;
import com.wiki.monowiki.wiki.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class VersionController {

    private final VersionService service;

    public VersionController(VersionService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/versions")
    public BaseResponse<VersionResponse> create(@PathVariable Long id,
	    @Valid @RequestBody CreateVersionRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Version added", false, service.create(id, req));
    }

    @GetMapping("/articles/{id}/versions")
    public BasePageResponse<VersionResponse> list(
	    @PathVariable Long id,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "20") int size,
	    @RequestParam(defaultValue = "versionNo,asc") String sort
    ) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(service.list(id, pageable), "Versions fetched");
    }

    @GetMapping("/articles/{id}/versions/{no}")
    public BaseResponse<VersionResponse> get(@PathVariable Long id, @PathVariable Integer no) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Version fetched", false, service.get(id, no));
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
