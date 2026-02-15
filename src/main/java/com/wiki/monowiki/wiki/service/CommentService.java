package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.wiki.dto.CommentDtos.*;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.VersionComment;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleVersionRepository;
import com.wiki.monowiki.wiki.repo.VersionCommentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
public class CommentService {

    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final VersionCommentRepository comments;
    private final ApplicationEventPublisher publisher;

    public CommentService(ArticleRepository articles, ArticleVersionRepository versions, VersionCommentRepository comments, ApplicationEventPublisher publisher) {
	this.articles = articles;
	this.versions = versions;
	this.comments = comments;
	this.publisher = publisher;
    }

    @Transactional
    public CommentResponse create(Long articleId, Integer versionNo, CreateCommentRequest req) {
	Article a = articles.findById(articleId).orElseThrow(() -> new NotFoundException("Article not found"));

	if (isViewer() && a.getStatus() != ArticleStatus.PUBLISHED) {
	    throw new NotFoundException("Article not found");
	}

	versions.findByArticleAndVersionNo(a, versionNo).orElseThrow(() -> new NotFoundException("Version not found"));

	VersionComment c = VersionComment.builder()
		.article(a)
		.versionNo(versionNo)
		.body(req.body().trim())
		.createdBy(currentUsername())
		.build();

	c = comments.save(c);

	boolean isPublic = a.getStatus() == ArticleStatus.PUBLISHED;

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.COMMENT_ADDED,
		com.wiki.monowiki.audit.model.AuditEntityType.COMMENT,
		c.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Added comment on version " + versionNo,
		isPublic,
		Map.of("commentId", c.getId(), "versionNo", versionNo)
	));

	return toDto(c);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> list(Long articleId, Integer versionNo, Pageable pageable) {
	Article a = articles.findById(articleId).orElseThrow(() -> new NotFoundException("Article not found"));

	if (isViewer() && a.getStatus() != ArticleStatus.PUBLISHED) {
	    throw new NotFoundException("Article not found");
	}

	versions.findByArticleAndVersionNo(a, versionNo).orElseThrow(() -> new NotFoundException("Version not found"));

	return comments.findByArticleAndVersionNo(a, versionNo, pageable)
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

    private boolean isViewer() {
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	if (a == null) return false;
	return a.getAuthorities().stream().anyMatch(g -> Objects.equals(g.getAuthority(), "ROLE_VIEWER"));
    }

    private String currentUsername() {
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	return (a != null) ? a.getName() : "system";
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
