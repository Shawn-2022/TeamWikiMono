package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.ArticleResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.CreateArticleRequest;
import com.wiki.monowiki.wiki.dto.ArticleDtos.UpdateTitleRequest;
import com.wiki.monowiki.wiki.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Articles", description = "Articles live inside a space and have statuses DRAFT / IN_REVIEW / PUBLISHED / ARCHIVED.")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
	this.articleService = articleService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/spaces/{spaceKey}/articles")
    @Operation(summary = "Create article (ADMIN/EDITOR)", description = "Creates a DRAFT article and automatically creates version #1.")
    public BaseResponse<ArticleResponse> create(@PathVariable String spaceKey,
	    @Valid @RequestBody CreateArticleRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article created", false, articleService.create(spaceKey, req));
    }

    @GetMapping("/spaces/{spaceKey}/articles")
    @Operation(
	    summary = "List articles in a space",
	    description = "VIEWERs only see PUBLISHED articles. For ADMIN/EDITOR, includeArchived=true includes soft-deleted articles."
    )
    public BasePageResponse<ArticleResponse> list(
	    @Parameter(description = "Space key", example = "ENG") @PathVariable String spaceKey,
	    @Parameter(description = "Include ARCHIVED articles (ADMIN/EDITOR only). Ignored for VIEWER.")
	    @RequestParam(defaultValue = "false") boolean includeArchived,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size,
	    @RequestParam(defaultValue = "createdAt,desc") String sort) {
	Pageable pageable = PageRequest.of(page, size, parseSort(sort));
	return BasePageResponse.fromPage(articleService.list(spaceKey, includeArchived, pageable), "Articles fetched");
    }

    @GetMapping("/spaces/{spaceKey}/articles/{slug}")
    @Operation(summary = "Get article by slug", description = "VIEWERs get 404 for non-PUBLISHED articles.")
    public BaseResponse<ArticleResponse> get(@PathVariable String spaceKey, @PathVariable String slug) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article fetched", false, articleService.getBySlug(spaceKey, slug));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PatchMapping("/articles/{id}")
    @Operation(summary = "Update draft article title (ADMIN/EDITOR)", description = "Only allowed for DRAFT articles.")
    public BaseResponse<ArticleResponse> updateTitle(@PathVariable Long id,
	    @Valid @RequestBody UpdateTitleRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article title updated", false, articleService.updateTitle(id, req));
    }

    /**
     * Optional (Nice-to-have): Soft delete
     */
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/archive")
    @Operation(summary = "Archive article (ADMIN/EDITOR)", description = "Soft delete. Not allowed while IN_REVIEW.")
    public BaseResponse<ArticleResponse> archive(@PathVariable Long id) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article archived", false, articleService.archive(id));
    }

    /**
     * Optional (Nice-to-have): Restore soft-deleted article back to DRAFT.
     */
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/unarchive")
    @Operation(summary = "Unarchive article (ADMIN/EDITOR)", description = "Restores an archived article back to DRAFT.")
    public BaseResponse<ArticleResponse> unarchive(@PathVariable Long id) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Article restored to draft", false, articleService.unarchive(id));
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
