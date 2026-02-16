package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.ArticleDtos.*;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repo.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ArticleService {

    public static final String SPACE_NOT_FOUND = "Space not found";
    public static final String ARTICLE_NOT_FOUND = "Article not found";
    private final ArticleRepository articles;
    private final SpaceRepository spaces;
    private final ArticleVersionRepository versions;
    private final ArticleTagRepository articleTags;
    private final VersionCommentRepository comments;
    private final ApplicationEventPublisher publisher;

    public ArticleService(ArticleRepository articles,
	    SpaceRepository spaces,
	    ArticleVersionRepository versions,
	    ArticleTagRepository articleTags,
	    VersionCommentRepository comments,
	    ApplicationEventPublisher publisher) {
	this.articles = articles;
	this.spaces = spaces;
	this.versions = versions;
	this.articleTags = articleTags;
	this.comments = comments;
	this.publisher = publisher;
    }

    @Transactional
    public ArticleResponse create(String spaceKey, CreateArticleRequest req) {
	Space space = spaces.findBySpaceKey(spaceKey)
		.orElseThrow(() -> new NotFoundException(SPACE_NOT_FOUND));

	String baseSlug = SlugUtil.slugify(req.title());
	String slug = ensureUniqueSlug(space, baseSlug);

	String actor = safeUsername();

	Article a = Article.builder()
		.space(space)
		.title(req.title().trim())
		.slug(slug)
		.status(ArticleStatus.DRAFT)
		.createdBy(actor)
		.build();

	a = articles.save(a);

	ArticleVersion v1 = ArticleVersion.builder()
		.article(a)
		.versionNo(1)
		.content(req.content())
		.createdBy(actor)
		.build();

	versions.save(v1);
	a.setCurrentVersionNo(1);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.ARTICLE_CREATED,
		com.wiki.monowiki.audit.model.AuditEntityType.ARTICLE,
		a.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Created article: " + a.getTitle(),
		false, // draft => not public
		Map.of("slug", a.getSlug(), "versionNo", 1)
	));

	return toResponse(a, v1.getContent());
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> list(String spaceKey, boolean includeArchived, Pageable pageable) {
	Space space = spaces.findBySpaceKey(spaceKey)
		.orElseThrow(() -> new NotFoundException(SPACE_NOT_FOUND));

	Page<Article> page;
	if (SecurityUtils.isViewer()) {
	    page = articles.findBySpaceAndStatus(space, ArticleStatus.PUBLISHED, pageable);
	} else if (includeArchived) {
	    page = articles.findBySpace(space, pageable);
	} else {
	    page = articles.findBySpaceAndStatusNot(space, ArticleStatus.ARCHIVED, pageable);
	}

	return page.map(a -> toResponse(a, latestContent(a)));
    }

    @Transactional(readOnly = true)
    public ArticleResponse getBySlug(String spaceKey, String slug) {
	Space space = spaces.findBySpaceKey(spaceKey)
		.orElseThrow(() -> new NotFoundException(SPACE_NOT_FOUND));

	Article a = articles.findBySpaceAndSlug(space, slug)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	if (SecurityUtils.isViewer() && a.getStatus() != ArticleStatus.PUBLISHED) {
	    throw new NotFoundException(ARTICLE_NOT_FOUND);
	}

	return toResponse(a, latestContent(a));
    }

    @Transactional
    public ArticleResponse updateTitle(Long articleId, UpdateTitleRequest req) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	// Business rule: only drafts are updatable (published/in-review must go through draft -> review flow)
	if (a.getStatus() != ArticleStatus.DRAFT) {
	    throw new IllegalArgumentException("Only draft articles can be updated");
	}

	String oldTitle = a.getTitle();
	a.setTitle(req.title().trim());

	boolean isPublic = false; // drafts are not public

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.ARTICLE_TITLE_UPDATED,
		com.wiki.monowiki.audit.model.AuditEntityType.ARTICLE,
		a.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Updated article title: " + a.getTitle(),
		isPublic,
		Map.of("slug", a.getSlug(), "oldTitle", oldTitle, "newTitle", a.getTitle())
	));

	return toResponse(a, latestContent(a));
    }

    /**
     * Optional (Nice-to-have): soft delete.
     */
    @Transactional
    public ArticleResponse archive(Long articleId) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	if (a.getStatus() == ArticleStatus.ARCHIVED) {
	    return toResponse(a, latestContent(a)); // idempotent
	}

	// Keep review workflow consistent (no dangling pending reviews).
	if (a.getStatus() == ArticleStatus.IN_REVIEW) {
	    throw new IllegalArgumentException("Cannot archive an article while it is in review");
	}

	ArticleStatus from = a.getStatus();
	a.setStatus(ArticleStatus.ARCHIVED);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.ARTICLE_ARCHIVED,
		com.wiki.monowiki.audit.model.AuditEntityType.ARTICLE,
		a.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Archived article: " + a.getTitle(),
		false,
		Map.of("slug", a.getSlug(), "fromStatus", String.valueOf(from))
	));

	return toResponse(a, latestContent(a));
    }

    /**
     * Optional (Nice-to-have): restore soft-deleted article back to DRAFT.
     */
    @Transactional
    public ArticleResponse unarchive(Long articleId) {
	Article a = articles.findById(articleId)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	if (a.getStatus() != ArticleStatus.ARCHIVED) {
	    throw new IllegalArgumentException("Only archived articles can be restored");
	}

	a.setStatus(ArticleStatus.DRAFT);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.ARTICLE_UNARCHIVED,
		com.wiki.monowiki.audit.model.AuditEntityType.ARTICLE,
		a.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Restored article to draft: " + a.getTitle(),
		false,
		Map.of("slug", a.getSlug())
	));

	return toResponse(a, latestContent(a));
    }

    // ---------- helpers ----------

    private String latestContent(Article a) {
	Integer cv = a.getCurrentVersionNo();
	if (cv == null || cv <= 0) return null;

	return versions.findByArticleAndVersionNo(a, cv)
		.map(ArticleVersion::getContent)
		.orElse(null);
    }

    private List<TagSummary> tagSummaries(Article a) {
	return articleTags.findByArticle(a).stream()
		.map(ArticleTag::getTag)
		.map(t -> new TagSummary(t.getId(), t.getName()))
		.toList();
    }

    private long currentVersionCommentCount(Article a) {
	Integer cv = a.getCurrentVersionNo();
	if (cv == null || cv <= 0) return 0L;
	return comments.countByArticleAndVersionNo(a, cv);
    }

    private String ensureUniqueSlug(Space space, String base) {
	String slug = base;
	int i = 2;
	while (articles.existsBySpaceAndSlug(space, slug)) {
	    String suffix = "-" + i++;
	    int maxLen = 140 - suffix.length();
	    String trimmed = base.length() > maxLen ? base.substring(0, maxLen).replaceAll("-+$", "") : base;
	    slug = trimmed + suffix;
	}
	return slug;
    }

    private String safeUsername() {
	String u = SecurityUtils.username();
	return (u == null || u.isBlank()) ? "system" : u;
    }

    private ArticleResponse toResponse(Article a, String latestContent) {
	return new ArticleResponse(
		a.getId(),
		a.getSpace().getSpaceKey(),
		a.getSlug(),
		a.getTitle(),
		a.getStatus(),
		a.getCurrentVersionNo(),
		latestContent,
		tagSummaries(a),
		currentVersionCommentCount(a),
		a.getCreatedBy(),
		a.getCreatedAt(),
		a.getUpdatedAt()
	);
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
