package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.*;
import com.wiki.monowiki.wiki.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/spaces/{spaceKey}/articles")
    public BaseResponse<ArticleResponse> create(@PathVariable String spaceKey,
	    @Valid @RequestBody CreateArticleRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article created", false, service.create(spaceKey, req));
    }

    @GetMapping("/spaces/{spaceKey}/articles")
    public BasePageResponse<ArticleResponse> list(@PathVariable String spaceKey,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size,
	    @RequestParam(defaultValue = "createdAt,desc") String sort) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(service.list(spaceKey, pageable), "Articles fetched");
    }

    @GetMapping("/spaces/{spaceKey}/articles/{slug}")
    public BaseResponse<ArticleResponse> get(@PathVariable String spaceKey, @PathVariable String slug) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article fetched", false, service.getBySlug(spaceKey, slug));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PatchMapping("/articles/{id}")
    public BaseResponse<ArticleResponse> updateTitle(@PathVariable Long id,
	    @Valid @RequestBody UpdateTitleRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article title updated", false, service.updateTitle(id, req));
    }

    private Sort parseSort(String sort) {
	try {
	    String[] parts = sort.split(",");
	    String field = parts[0].trim();
	    Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
		    ? Sort.Direction.ASC : Sort.Direction.DESC;
	    return Sort.by(dir, field);
	} catch (Exception e) {
	    return Sort.by(Sort.Direction.DESC, "createdAt");
	}
    }
}
