package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.TagDtos.*;
import com.wiki.monowiki.wiki.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Tags", description = "Tag catalog and article-tag assignments.")
public class TagController {

    private final TagService service;

    public TagController(TagService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/tags")
    @Operation(summary = "Create tag (ADMIN/EDITOR)")
    public BaseResponse<TagResponse> create(@Valid @RequestBody CreateTagRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Tag created", false, service.create(req));
    }

    @GetMapping("/tags")
    @Operation(summary = "List tags (all roles)")
    public BasePageResponse<TagResponse> list(
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "20") int size,
	    @RequestParam(defaultValue = "name,asc") String sort
    ) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(service.list(pageable), "Tags fetched");
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/tags")
    @Operation(summary = "Attach tag to article (ADMIN/EDITOR)")
    public BaseResponse<Object> addToArticle(@PathVariable Long id,
	    @Valid @RequestBody AddTagToArticleRequest req) {
	service.addTagToArticle(id, req.tagId());
	return new BaseResponse<>(HttpStatus.OK.value(), "Tag added to article", false, null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @DeleteMapping("/articles/{id}/tags/{tagId}")
    @Operation(summary = "Remove tag from article (ADMIN/EDITOR)")
    public BaseResponse<Object> removeFromArticle(@PathVariable Long id, @PathVariable Long tagId) {
	service.removeTagFromArticle(id, tagId);
	return new BaseResponse<>(HttpStatus.OK.value(), "Tag removed from article", false, null);
    }

    private Sort parseSort(String sort) {
	try {
	    String[] parts = sort.split(",");
	    String field = parts[0].trim();
	    Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
		    ? Sort.Direction.DESC : Sort.Direction.ASC;
	    return Sort.by(dir, field);
	} catch (Exception e) {
	    return Sort.by(Sort.Direction.ASC, "name");
	}
    }
}
