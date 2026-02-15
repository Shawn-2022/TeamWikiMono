package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.SpaceDtos.*;
import com.wiki.monowiki.wiki.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService service;

    public SpaceController(SpaceService service) {
	this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public BaseResponse<SpaceResponse> create(@Valid @RequestBody CreateSpaceRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Space created", false, service.create(req));
    }

    @GetMapping
    public BasePageResponse<SpaceResponse> list(
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size,
	    @RequestParam(defaultValue = "spaceKey,asc") String sort
    ) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(service.list(pageable), "Spaces fetched");
    }

    @GetMapping("/{spaceKey}")
    public BaseResponse<SpaceResponse> get(@PathVariable String spaceKey) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Space fetched", false, service.getByKey(spaceKey));
    }

    private Sort parseSort(String sort) {
	try {
	    String[] parts = sort.split(",");
	    String field = parts[0].trim();
	    Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
		    ? Sort.Direction.DESC : Sort.Direction.ASC;
	    return Sort.by(dir, field);
	} catch (Exception e) {
	    return Sort.by(Sort.Direction.ASC, "spaceKey");
	}
    }
}
