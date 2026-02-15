package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.wiki.dto.VersionDtos.*;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ArticleVersion;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleVersionRepository;
import com.wiki.monowiki.common.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionService {

    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ApplicationEventPublisher publisher;

    public VersionService(ArticleRepository articles, ArticleVersionRepository versions, ApplicationEventPublisher publisher) {
	this.articles = articles;
	this.versions = versions;
	this.publisher = publisher;
    }

    @Transactional
    public VersionResponse create(Long articleId, CreateVersionRequest req) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException("Article not found"));

	if (a.getStatus() != ArticleStatus.DRAFT) {
	    throw new IllegalArgumentException("Versions can only be added while article is in DRAFT");
	}

	int nextNo = versions.findTopByArticleOrderByVersionNoDesc(a)
		.map(v -> v.getVersionNo() + 1)
		.orElse(1);

	ArticleVersion v = ArticleVersion.builder()
		.article(a)
		.versionNo(nextNo)
		.content(req.content())
		.createdBy(currentUsername())
		.build();

	v = versions.save(v);

	a.setCurrentVersionNo(nextNo);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.VERSION_ADDED,
		com.wiki.monowiki.audit.model.AuditEntityType.VERSION,
		v.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Added version " + v.getVersionNo() + " to article",
		false,
		java.util.Map.of("versionNo", v.getVersionNo())
	));

	return toResponse(v);
    }

    @Transactional(readOnly = true)
    public Page<VersionResponse> list(Long articleId, Pageable pageable) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException("Article not found"));

	if (SecurityUtils.isViewer() && a.getStatus() != ArticleStatus.PUBLISHED) {
	    throw new NotFoundException("Article not found");
	}

	return versions.findByArticle(a, pageable)
		.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VersionResponse get(Long articleId, Integer versionNo) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException("Article not found"));

	if (SecurityUtils.isViewer() && a.getStatus() != ArticleStatus.PUBLISHED) {
	    throw new NotFoundException("Article not found");
	}

	ArticleVersion v = versions.findByArticleAndVersionNo(a, versionNo)
		.orElseThrow(() -> new NotFoundException("Version not found"));

	return toResponse(v);
    }

    private String currentUsername() {
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	return (a != null) ? a.getName() : "system";
    }

    private VersionResponse toResponse(ArticleVersion v) {
	return new VersionResponse(
		v.getId(),
		v.getArticle().getId(),
		v.getVersionNo(),
		v.getContent(),
		v.getCreatedBy(),
		v.getCreatedAt()
	);
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
