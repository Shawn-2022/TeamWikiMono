package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.wiki.dto.ReviewDtos.*;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ReviewRequest;
import com.wiki.monowiki.wiki.model.ReviewStatus;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ReviewRequestRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ReviewService {

    private final ArticleRepository articles;
    private final ReviewRequestRepository reviews;
    private final ApplicationEventPublisher publisher;

    public ReviewService(ArticleRepository articles, ReviewRequestRepository reviews, ApplicationEventPublisher publisher) {
	this.articles = articles;
	this.reviews = reviews;
	this.publisher = publisher;
    }

    @Transactional
    public ReviewRequestResponse submit(Long articleId) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException("Article not found"));

	if (a.getStatus() != ArticleStatus.DRAFT) {
	    throw new IllegalArgumentException("Only DRAFT articles can be submitted for review");
	}

	if (reviews.existsByArticleIdAndStatus(articleId, ReviewStatus.PENDING)) {
	    throw new IllegalArgumentException("A pending review request already exists for this article");
	}

	String actor = currentUsername();

	ReviewRequest rr = ReviewRequest.builder()
		.article(a)
		.status(ReviewStatus.PENDING)
		.requestedBy(actor)
		.build();

	rr = reviews.save(rr);

	a.setStatus(ArticleStatus.IN_REVIEW);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.REVIEW_SUBMITTED,
		com.wiki.monowiki.audit.model.AuditEntityType.REVIEW_REQUEST,
		rr.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Submitted review request for article",
		false,
		Map.of("reviewRequestId", rr.getId())
	));

	return toResponse(rr);
    }

    @Transactional(readOnly = true)
    public Page<ReviewRequestResponse> list(Integer page, Integer size, String status) {
	Pageable pageable = PageRequest.of(
		page != null ? page : 0,
		size != null ? size : 10,
		Sort.by(Sort.Direction.DESC, "requestedAt")
	);

	Page<ReviewRequest> result;
	if (status != null && !status.isBlank()) {
	    ReviewStatus rs = ReviewStatus.valueOf(status.trim().toUpperCase());
	    result = reviews.findByStatus(rs, pageable);
	} else {
	    result = reviews.findAll(pageable);
	}

	return result.map(this::toResponse);
    }

    @Transactional
    public ReviewRequestResponse approve(Long reviewRequestId) {
	ReviewRequest rr = reviews.findById(reviewRequestId)
		.orElseThrow(() -> new NotFoundException("Review request not found"));

	if (rr.getStatus() != ReviewStatus.PENDING) {
	    throw new IllegalArgumentException("Only PENDING review requests can be approved");
	}

	rr.setStatus(ReviewStatus.APPROVED);
	rr.setReviewedBy(currentUsername());
	rr.setReviewedAt(Instant.now());

	Article a = rr.getArticle();
	a.setStatus(ArticleStatus.PUBLISHED);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.REVIEW_APPROVED,
		com.wiki.monowiki.audit.model.AuditEntityType.REVIEW_REQUEST,
		rr.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Approved review request (article published)",
		true, // publish moment => public
		Map.of("reviewRequestId", rr.getId())
	));

	return toResponse(rr);
    }

    @Transactional
    public ReviewRequestResponse reject(Long reviewRequestId, RejectRequest req) {
	ReviewRequest rr = reviews.findById(reviewRequestId)
		.orElseThrow(() -> new NotFoundException("Review request not found"));

	if (rr.getStatus() != ReviewStatus.PENDING) {
	    throw new IllegalArgumentException("Only PENDING review requests can be rejected");
	}

	rr.setStatus(ReviewStatus.REJECTED);
	rr.setReviewedBy(currentUsername());
	rr.setReviewedAt(Instant.now());
	rr.setReason(req.reason().trim());

	Article a = rr.getArticle();
	a.setStatus(ArticleStatus.DRAFT);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.REVIEW_REJECTED,
		com.wiki.monowiki.audit.model.AuditEntityType.REVIEW_REQUEST,
		rr.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Rejected review request (article back to draft)",
		false,
		Map.of("reviewRequestId", rr.getId(), "reason", rr.getReason())
	));

	return toResponse(rr);
    }

    private String currentUsername() {
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	return (a != null) ? a.getName() : "system";
    }

    private ReviewRequestResponse toResponse(ReviewRequest rr) {
	Article a = rr.getArticle();
	return new ReviewRequestResponse(
		rr.getId(),
		a.getId(),
		a.getSlug(),
		a.getSpace().getSpaceKey(),
		a.getStatus(),
		rr.getStatus(),
		rr.getRequestedBy(),
		rr.getRequestedAt(),
		rr.getReviewedBy(),
		rr.getReviewedAt(),
		rr.getReason()
	);
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
