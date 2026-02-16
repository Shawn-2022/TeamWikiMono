package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.ArticleResponse;
import com.wiki.monowiki.wiki.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Search", description = "Search across article title and latest version content.")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
	this.searchService = searchService;
    }

    @GetMapping("/search")
    @Operation(
	    summary = "Search articles",
	    description = "VIEWERs only see PUBLISHED articles. For ADMIN/EDITOR, set includeArchived=true to include soft-deleted articles."
    )
    public BasePageResponse<ArticleResponse> search(
	    @Parameter(description = "Space key to search within", example = "ENG") @RequestParam String spaceKey,
	    @Parameter(description = "Query string (searched in title and latest content)", example = "onboarding") @RequestParam String q,
	    @Parameter(description = "Include ARCHIVED articles (ADMIN/EDITOR only). Ignored for VIEWER.")
	    @RequestParam(defaultValue = "false") boolean includeArchived,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size) {
	Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
	return BasePageResponse.fromPage(searchService.search(spaceKey, q, includeArchived, pageable), "Search results fetched");
    }
}
