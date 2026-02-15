package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.ArticleResponse;
import com.wiki.monowiki.wiki.service.SearchService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
	this.service = service;
    }

    @GetMapping("/search")
    public BasePageResponse<ArticleResponse> search(@RequestParam String spaceKey,
	    @RequestParam String q,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size) {
	Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
	return BasePageResponse.fromPage(service.search(spaceKey, q, pageable), "Search results fetched");
    }
}
