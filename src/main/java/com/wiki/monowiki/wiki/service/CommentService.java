package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.AuditActor;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.CommentDtos.CommentResponse;
import com.wiki.monowiki.wiki.dto.CommentDtos.CreateCommentRequest;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.VersionComment;
import com.wiki.monowiki.wiki.repository.ArticleRepository;
import com.wiki.monowiki.wiki.repository.ArticleVersionRepository;
import com.wiki.monowiki.wiki.repository.VersionCommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class CommentService {

    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final VersionCommentRepository versionCommentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CommentService(ArticleRepository articleRepository, ArticleVersionRepository articleVersionRepository, VersionCommentRepository versionCommentRepository, ApplicationEventPublisher applicationEventPublisher) {
	this.articleRepository = articleRepository;
	this.articleVersionRepository = articleVersionRepository;
	this.versionCommentRepository = versionCommentRepository;
	this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public CommentResponse create(Long articleId, Integer versionNo, CreateCommentRequest req) {
        log.info("Creating comment for articleId={}, versionNo={} by user={}", articleId, versionNo, SecurityUtils.username());
	Article a = articleRepository.findById(articleId).orElseThrow(() -> {
            log.warn("Article not found for id={} during comment create", articleId);
            return new NotFoundException("Article not found");
        });

	if (SecurityUtils.isViewer() && !Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED)) {
            log.warn("Access denied for viewer to comment on non-published articleId={}", articleId);
	    throw new NotFoundException("Article not found");
	}

	articleVersionRepository.findByArticleAndVersionNo(a, versionNo).orElseThrow(() -> {
            log.warn("Version {} not found for articleId={} during comment create", versionNo, articleId);
            return new NotFoundException("Version not found");
        });

	VersionComment c = VersionComment.builder()
		.article(a)
		.versionNo(versionNo)
		.body(req.body().trim())
		.createdBy(SecurityUtils.username())
		.build();

	c = versionCommentRepository.save(c);

	boolean isPublic = Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED);

	log.info("Comment {} created for articleId={}, versionNo={} by user={}", c.getId(), articleId, versionNo, c.getCreatedBy());

	applicationEventPublisher.publishEvent(new WikiAuditEvent(
		AuditEventType.COMMENT_ADDED,
		AuditEntityType.COMMENT,
		c.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		AuditActor.username(),
		"Added comment on version " + versionNo,
		isPublic,
		Map.of("commentId", c.getId(), "versionNo", versionNo)
	));

	return toDto(c);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> list(Long articleId, Integer versionNo, Pageable pageable) {
        log.info("Listing comments for articleId={}, versionNo={} by user={}", articleId, versionNo, SecurityUtils.username());
	Article a = articleRepository.findById(articleId).orElseThrow(() -> {
            log.warn("Article not found for id={} during comment list", articleId);
            return new NotFoundException("Article not found");
        });

	if (SecurityUtils.isViewer() && !Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED)) {
            log.warn("Access denied for viewer to list comments on non-published articleId={}", articleId);
	    throw new NotFoundException("Article not found");
	}

	articleVersionRepository.findByArticleAndVersionNo(a, versionNo).orElseThrow(() -> {
            log.warn("Version {} not found for articleId={} during comment list", versionNo, articleId);
            return new NotFoundException("Version not found");
        });

        log.debug("Fetching comments for articleId={}, versionNo={} with pageable={}", articleId, versionNo, pageable);
	return versionCommentRepository.findByArticleAndVersionNo(a, versionNo, pageable)
		.map(this::toDto);
    }

    private CommentResponse toDto(VersionComment c) {
	return new CommentResponse(
		c.getId(),
		c.getArticle().getId(),
		c.getVersionNo(),
		c.getBody(),
		c.getCreatedBy(),
		c.getCreatedAt()
	);
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
