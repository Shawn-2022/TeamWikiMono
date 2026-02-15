package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.ReviewDtos.*;
import com.wiki.monowiki.wiki.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/review-requests")
    public BaseResponse<ReviewRequestResponse> submit(@PathVariable Long id) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Review request submitted", false, service.submit(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/review-requests")
    public BasePageResponse<ReviewRequestResponse> list(@RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size,
	    @RequestParam(required = false) String status) {
	return BasePageResponse.fromPage(service.list(page, size, status), "Review requests fetched");
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/review-requests/{id}/approve")
    public BaseResponse<ReviewRequestResponse> approve(@PathVariable Long id) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Review approved", false, service.approve(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/review-requests/{id}/reject")
    public BaseResponse<ReviewRequestResponse> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Review rejected", false, service.reject(id, req));
    }
}
