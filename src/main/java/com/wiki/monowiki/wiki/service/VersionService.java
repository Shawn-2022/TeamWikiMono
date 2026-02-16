package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.AuditActor;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.VersionDtos.CreateVersionRequest;
import com.wiki.monowiki.wiki.dto.VersionDtos.VersionResponse;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ArticleVersion;
import com.wiki.monowiki.wiki.repository.ArticleRepository;
import com.wiki.monowiki.wiki.repository.ArticleVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class VersionService {

    public static final String ARTICLE_NOT_FOUND = "Article not found";
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
        log.info("Creating new version for articleId={} by user={}", articleId, currentUsername());
        Article a = articles.findById(articleId)
                .orElseThrow(() -> {
                    log.warn("Article not found for id={} during version creation", articleId);
                    return new NotFoundException(ARTICLE_NOT_FOUND);
                });

        if (!Objects.equals(a.getStatus(), ArticleStatus.DRAFT)) {
            log.warn("VERSION_ADD_BLOCKED: articleId={} actor={} status={}",
                    a.getId(), SecurityUtils.username(), a.getStatus());
            throw new IllegalArgumentException("Versions can only be added while article is in DRAFT");
        }

        int nextNo = versions.findTopByArticleOrderByVersionNoDesc(a)
                .map(v -> v.getVersionNo() + 1)
                .orElse(1);

        log.debug("Next version number for articleId={}: {}", articleId, nextNo);

        ArticleVersion v = ArticleVersion.builder()
                .article(a)
                .versionNo(nextNo)
                .content(req.content())
                .createdBy(currentUsername())
                .build();

        v = versions.save(v);

        a.setCurrentVersionNo(nextNo);

        log.info("Version {} created for articleId={} by user={}", v.getVersionNo(), articleId, v.getCreatedBy());

        publishVersionEvent(
            AuditEventType.VERSION_ADDED,
            v,
            a,
            "Added version " + v.getVersionNo() + " to article",
            false,
            java.util.Map.of("versionNo", v.getVersionNo())
        );

        return toResponse(v);
    }

    @Transactional(readOnly = true)
    public Page<VersionResponse> list(Long articleId, Pageable pageable) {
        log.info("Listing versions for articleId={} by user={}", articleId, currentUsername());
        Article a = articles.findById(articleId)
                .orElseThrow(() -> {
                    log.warn("Article not found for id={} during version list", articleId);
                    return new NotFoundException(ARTICLE_NOT_FOUND);
                });

        if (SecurityUtils.isViewer() && !Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED)) {
            log.warn("Access denied for viewer to list versions of non-published articleId={}", articleId);
            throw new NotFoundException(ARTICLE_NOT_FOUND);
        }

        log.debug("Fetching versions for articleId={} with pageable={}", articleId, pageable);
        return versions.findByArticle(a, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VersionResponse get(Long articleId, Integer versionNo) {
        log.info("Getting version {} for articleId={} by user={}", versionNo, articleId, currentUsername());
        Article a = articles.findById(articleId)
                .orElseThrow(() -> {
                    log.warn("Article not found for id={} during version get", articleId);
                    return new NotFoundException(ARTICLE_NOT_FOUND);
                });

        if (SecurityUtils.isViewer() && !Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED)) {
            log.warn("Access denied for viewer to get version of non-published articleId={}", articleId);
            throw new NotFoundException(ARTICLE_NOT_FOUND);
        }

        ArticleVersion v = versions.findByArticleAndVersionNo(a, versionNo)
                .orElseThrow(() -> {
                    log.warn("Version {} not found for articleId={}", versionNo, articleId);
                    return new NotFoundException("Version not found");
                });

        log.info("Returning version {} for articleId={}", versionNo, articleId);
        return toResponse(v);
    }

    private void publishVersionEvent(
            AuditEventType eventType,
            ArticleVersion version,
            Article article,
            String message,
            boolean isPublic,
            java.util.Map<String, Object> details
    ) {
        publisher.publishEvent(new WikiAuditEvent(
                eventType,
                AuditEntityType.VERSION,
                version.getId(),
                article.getSpace().getSpaceKey(),
                article.getId(),
                AuditActor.username(),
                message,
                isPublic,
                details
        ));
    }

    private String currentUsername() {
        String u = SecurityUtils.username();
        return (Objects.isNull(u) || u.isBlank()) ? "system" : u;
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
