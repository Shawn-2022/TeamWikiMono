package com.wiki.monowiki.wiki.controller;

import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.response.BaseResponse;
import com.wiki.monowiki.wiki.dto.ReviewDtos.*;
import com.wiki.monowiki.wiki.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Review workflow", description = "Submit drafts for review and approve/reject to publish.")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/articles/{id}/review-requests")
    @Operation(summary = "Submit article for review (ADMIN/EDITOR)")
    public BaseResponse<ReviewRequestResponse> submit(@PathVariable Long id) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Review request submitted", false, service.submit(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/review-requests")
    @Operation(summary = "List review requests (ADMIN/EDITOR)")
    public BasePageResponse<ReviewRequestResponse> list(@RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "10") int size,
	    @RequestParam(required = false) String status) {
	return BasePageResponse.fromPage(service.list(page, size, status), "Review requests fetched");
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/review-requests/{id}/approve")
    @Operation(summary = "Approve review request (ADMIN/EDITOR)")
    public BaseResponse<ReviewRequestResponse> approve(@PathVariable Long id) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Review approved", false, service.approve(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/review-requests/{id}/reject")
    @Operation(summary = "Reject review request (ADMIN/EDITOR)")
    public BaseResponse<ReviewRequestResponse> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
	return new BaseResponse<>(HttpStatus.OK.value(), "Review rejected", false, service.reject(id, req));
    }
}
