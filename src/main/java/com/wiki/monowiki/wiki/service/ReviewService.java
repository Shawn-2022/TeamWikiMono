package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.ReviewDtos.RejectRequest;
import com.wiki.monowiki.wiki.dto.ReviewDtos.ReviewRequestResponse;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ReviewRequest;
import com.wiki.monowiki.wiki.model.ReviewStatus;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ReviewRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class ReviewService {

    public static final String REVIEW_REQUEST_ID = "reviewRequestId";
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
        log.info("Submitting review request for articleId={} by user={}", articleId, SecurityUtils.username());
	Article a = articles.findById(articleId)
		.orElseThrow(() -> {
                log.warn("Article not found for id={} during review submit", articleId);
                return new NotFoundException("Article not found");
            });

	if (a.getStatus() != ArticleStatus.DRAFT) {
            log.warn("Review submit blocked: articleId={} status={}", articleId, a.getStatus());
	    throw new IllegalArgumentException("Only DRAFT articles can be submitted for review");
	}

	if (reviews.existsByArticleIdAndStatus(articleId, ReviewStatus.PENDING)) {
            log.warn("Duplicate review submit attempt: articleId={}", articleId);
	    throw new IllegalArgumentException("A pending review request already exists for this article");
	}

	String actor = SecurityUtils.username();

	ReviewRequest rr = ReviewRequest.builder()
		.article(a)
		.status(ReviewStatus.PENDING)
		.requestedBy(actor)
		.build();

	rr = reviews.save(rr);

	a.setStatus(ArticleStatus.IN_REVIEW);

	log.info("Review request {} submitted for articleId={} by user={}", rr.getId(), articleId, actor);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.REVIEW_SUBMITTED,
		com.wiki.monowiki.audit.model.AuditEntityType.REVIEW_REQUEST,
		rr.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Submitted review request for article",
		false,
		Map.of(REVIEW_REQUEST_ID, rr.getId())
	));

	return toResponse(rr);
    }

    @Transactional(readOnly = true)
    public Page<ReviewRequestResponse> list(Integer page, Integer size, String status) {
        log.info("Listing review requests: page={}, size={}, status={}, by user={}", page, size, status, SecurityUtils.username());
	Pageable pageable = PageRequest.of(
		page != null ? page : 0,
		size != null ? size : 10,
		Sort.by(Sort.Direction.DESC, "requestedAt")
	);

	Page<ReviewRequest> result;
	if (status != null && !status.isBlank()) {
	    ReviewStatus rs = ReviewStatus.valueOf(status.trim().toUpperCase());
            log.debug("Filtering review requests by status={}", rs);
	    result = reviews.findByStatus(rs, pageable);
	} else {
            log.debug("Listing all review requests");
	    result = reviews.findAll(pageable);
	}

	return result.map(this::toResponse);
    }

    @Transactional
    public ReviewRequestResponse approve(Long reviewRequestId) {
        log.info("Approving review request {} by user={}", reviewRequestId, SecurityUtils.username());
	ReviewRequest rr = reviews.findById(reviewRequestId)
		.orElseThrow(() -> {
                log.warn("Review request not found for id={} during approve", reviewRequestId);
                return new NotFoundException("Review request not found");
            });

	if (rr.getStatus() != ReviewStatus.PENDING) {
            log.warn("Approve blocked: reviewRequestId={} status={}", reviewRequestId, rr.getStatus());
	    throw new IllegalArgumentException("Only PENDING review requests can be approved");
	}

	rr.setStatus(ReviewStatus.APPROVED);
	rr.setReviewedBy(SecurityUtils.username());
	rr.setReviewedAt(Instant.now());

	Article a = rr.getArticle();
	a.setStatus(ArticleStatus.PUBLISHED);

	log.info("Review request {} approved and article {} published by user={}", reviewRequestId, a.getId(), SecurityUtils.username());

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.REVIEW_APPROVED,
		com.wiki.monowiki.audit.model.AuditEntityType.REVIEW_REQUEST,
		rr.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Approved review request (article published)",
		true, // publish moment => public
		Map.of(REVIEW_REQUEST_ID, rr.getId())
	));

	return toResponse(rr);
    }

    @Transactional
    public ReviewRequestResponse reject(Long reviewRequestId, RejectRequest req) {
        log.info("Rejecting review request {} by user={}", reviewRequestId, SecurityUtils.username());
	ReviewRequest rr = reviews.findById(reviewRequestId)
		.orElseThrow(() -> {
                log.warn("Review request not found for id={} during reject", reviewRequestId);
                return new NotFoundException("Review request not found");
            });

	if (rr.getStatus() != ReviewStatus.PENDING) {
            log.warn("Reject blocked: reviewRequestId={} status={}", reviewRequestId, rr.getStatus());
	    throw new IllegalArgumentException("Only PENDING review requests can be rejected");
	}

	rr.setStatus(ReviewStatus.REJECTED);
	rr.setReviewedBy(SecurityUtils.username());
	rr.setReviewedAt(Instant.now());
	rr.setReason(req.reason().trim());

	Article a = rr.getArticle();
	a.setStatus(ArticleStatus.DRAFT);

	log.info("Review request {} rejected and article {} set to draft by user={}", reviewRequestId, a.getId(), SecurityUtils.username());

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.REVIEW_REJECTED,
		com.wiki.monowiki.audit.model.AuditEntityType.REVIEW_REQUEST,
		rr.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Rejected review request (article back to draft)",
		false,
		Map.of(REVIEW_REQUEST_ID, rr.getId(), "reason", rr.getReason())
	));

	return toResponse(rr);
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
