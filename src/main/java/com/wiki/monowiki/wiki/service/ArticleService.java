package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.AuditActor;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.ArticleDtos.ArticleResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.CreateArticleRequest;
import com.wiki.monowiki.wiki.dto.ArticleDtos.TagSummary;
import com.wiki.monowiki.wiki.dto.ArticleDtos.UpdateTitleRequest;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.wiki.monowiki.audit.model.AuditEntityType.ARTICLE;
import static com.wiki.monowiki.audit.model.AuditEventType.*;

@Slf4j
@Service
public class ArticleService {

    public static final String SPACE_NOT_FOUND = "Space not found";
    public static final String ARTICLE_NOT_FOUND = "Article not found";
    private final ArticleRepository articleRepository;
    private final SpaceRepository spaceRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final ArticleTagRepository articleTagRepository;
    private final VersionCommentRepository versionCommentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ArticleService(ArticleRepository articleRepository,
	    SpaceRepository spaceRepository,
	    ArticleVersionRepository articleVersionRepository,
	    ArticleTagRepository articleTagRepository,
	    VersionCommentRepository versionCommentRepository,
	    ApplicationEventPublisher applicationEventPublisher) {
	this.articleRepository = articleRepository;
	this.spaceRepository = spaceRepository;
	this.articleVersionRepository = articleVersionRepository;
	this.articleTagRepository = articleTagRepository;
	this.versionCommentRepository = versionCommentRepository;
	this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public ArticleResponse create(String spaceKey, CreateArticleRequest req) {
	Space space = spaceRepository.findBySpaceKey(spaceKey)
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

	a = articleRepository.save(a);

	ArticleVersion v1 = ArticleVersion.builder()
		.article(a)
		.versionNo(1)
		.content(req.content())
		.createdBy(actor)
		.build();

	articleVersionRepository.save(v1);
	a.setCurrentVersionNo(1);

	log.info("ARTICLE_CREATED: articleId={} spaceKey={} slug={} actor={} status={}",
		a.getId(), a.getSpace().getSpaceKey(), a.getSlug(), actor, a.getStatus());

	publishArticleEvent(
		ARTICLE_CREATED,
		a,
		"Created article: " + a.getTitle(),
		false,
		Map.of("slug", a.getSlug(), "versionNo", 1)
	);

	return toResponse(a, v1.getContent());
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> list(String spaceKey, boolean includeArchived, Pageable pageable) {
	Space space = spaceRepository.findBySpaceKey(spaceKey)
		.orElseThrow(() -> new NotFoundException(SPACE_NOT_FOUND));

	Page<Article> page;
	if (SecurityUtils.isViewer()) {
	    page = articleRepository.findBySpaceAndStatus(space, ArticleStatus.PUBLISHED, pageable);
	} else if (includeArchived) {
	    page = articleRepository.findBySpace(space, pageable);
	} else {
	    page = articleRepository.findBySpaceAndStatusNot(space, ArticleStatus.ARCHIVED, pageable);
	}

	return page.map(a -> toResponse(a, latestContent(a)));
    }

    @Transactional(readOnly = true)
    public ArticleResponse getBySlug(String spaceKey, String slug) {
	Space space = spaceRepository.findBySpaceKey(spaceKey)
		.orElseThrow(() -> new NotFoundException(SPACE_NOT_FOUND));

	Article a = articleRepository.findBySpaceAndSlug(space, slug)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	if (SecurityUtils.isViewer() && a.getStatus() != ArticleStatus.PUBLISHED) {
	    throw new NotFoundException(ARTICLE_NOT_FOUND);
	}

	return toResponse(a, latestContent(a));
    }

    @Transactional
    public ArticleResponse updateTitle(Long articleId, UpdateTitleRequest req) {
	Article a = articleRepository.findById(articleId)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	// Business rule: only drafts are updatable (published/in-review must go through draft -> review flow)
	if (a.getStatus() != ArticleStatus.DRAFT) {
	    log.warn("ARTICLE_TITLE_UPDATE_BLOCKED: articleId={} actor={} status={}",
		    a.getId(), safeUsername(), a.getStatus());
	    throw new IllegalArgumentException("Only draft articles can be updated");
	}

	String oldTitle = a.getTitle();
	a.setTitle(req.title().trim());

	boolean isPublic = false; // drafts are not public

	log.info("ARTICLE_TITLE_UPDATED: articleId={} actor={} oldTitle='{}' newTitle='{}'",
		a.getId(), safeUsername(), safeShort(oldTitle), safeShort(a.getTitle()));

	publishArticleEvent(
		ARTICLE_TITLE_UPDATED,
		a,
		"Updated article title: " + a.getTitle(),
		isPublic,
		Map.of("slug", a.getSlug(), "oldTitle", oldTitle, "newTitle", a.getTitle())
	);

	return toResponse(a, latestContent(a));
    }

    /**
     * Optional (Nice-to-have): soft delete.
     */
    @Transactional
    public ArticleResponse archive(Long articleId) {
	Article a = articleRepository.findById(articleId)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	if (a.getStatus() == ArticleStatus.ARCHIVED) {
	    log.info("ARTICLE_ARCHIVE_IDEMPOTENT: articleId={} actor={}", a.getId(), safeUsername());
	    return toResponse(a, latestContent(a)); // idempotent
	}

	// Keep review workflow consistent (no dangling pending reviews).
	if (a.getStatus() == ArticleStatus.IN_REVIEW) {
	    log.warn("ARTICLE_ARCHIVE_BLOCKED_IN_REVIEW: articleId={} actor={}", a.getId(), safeUsername());
	    throw new IllegalArgumentException("Cannot archive an article while it is in review");
	}

	ArticleStatus from = a.getStatus();
	a.setStatus(ArticleStatus.ARCHIVED);

	log.info("ARTICLE_ARCHIVED: articleId={} spaceKey={} slug={} actor={} fromStatus={} toStatus={}",
		a.getId(), a.getSpace().getSpaceKey(), a.getSlug(), safeUsername(), from, a.getStatus());

	publishArticleEvent(
		ARTICLE_ARCHIVED,
		a,
		"Archived article: " + a.getTitle(),
		false,
		Map.of("slug", a.getSlug(), "fromStatus", String.valueOf(from))
	);

	return toResponse(a, latestContent(a));
    }

    /**
     * Optional (Nice-to-have): restore soft-deleted article back to DRAFT.
     */
    @Transactional
    public ArticleResponse unarchive(Long articleId) {
	Article a = articleRepository.findById(articleId)
		.orElseThrow(() -> new NotFoundException(ARTICLE_NOT_FOUND));

	if (a.getStatus() != ArticleStatus.ARCHIVED) {
	    log.warn("ARTICLE_UNARCHIVE_BLOCKED: articleId={} actor={} status={}",
		    a.getId(), safeUsername(), a.getStatus());
	    throw new IllegalArgumentException("Only archived articles can be restored");
	}

	a.setStatus(ArticleStatus.DRAFT);

	log.info("ARTICLE_UNARCHIVED: articleId={} spaceKey={} slug={} actor={} toStatus={}",
		a.getId(), a.getSpace().getSpaceKey(), a.getSlug(), safeUsername(), a.getStatus());

	publishArticleEvent(
		ARTICLE_UNARCHIVED,
		a,
		"Restored article to draft: " + a.getTitle(),
		false,
		Map.of("slug", a.getSlug())
	);

	return toResponse(a, latestContent(a));
    }

    // ---------- helpers ----------

    private String latestContent(Article a) {
        Integer cv = a.getCurrentVersionNo();
        if (Objects.isNull(cv) || cv <= 0) return null;
        return articleVersionRepository.findByArticleAndVersionNo(a, cv)
                .map(ArticleVersion::getContent)
                .orElse(null);
    }

    private List<TagSummary> tagSummaries(Article a) {
	return articleTagRepository.findByArticle(a).stream()
		.map(ArticleTag::getTag)
		.map(t -> new TagSummary(t.getId(), t.getName()))
		.toList();
    }

    private long currentVersionCommentCount(Article a) {
        Integer cv = a.getCurrentVersionNo();
        if (Objects.isNull(cv) || cv <= 0) return 0L;
        return versionCommentRepository.countByArticleAndVersionNo(a, cv);
    }

    private String ensureUniqueSlug(Space space, String base) {
	String slug = base;
	int i = 2;
	while (articleRepository.existsBySpaceAndSlug(space, slug)) {
	    String suffix = "-" + i++;
	    int maxLen = 140 - suffix.length();
	    String trimmed = base.length() > maxLen ? base.substring(0, maxLen).replaceAll("-+$", "") : base;
	    slug = trimmed + suffix;
	}
	return slug;
    }

    private String safeUsername() {
        String u = SecurityUtils.username();
        return (Objects.isNull(u) || u.isBlank()) ? "system" : u;
    }

    private String safeShort(String s) {
        if (Objects.isNull(s)) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= 120 ? t : t.substring(0, 120) + "...";
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

    private void publishArticleEvent(
            AuditEventType eventType,
            Article article,
            String message,
            boolean isPublic,
            Map<String, Object> details
    ) {
        applicationEventPublisher.publishEvent(new WikiAuditEvent(
                eventType,
                ARTICLE,
                article.getId(),
                article.getSpace().getSpaceKey(),
                article.getId(),
              	AuditActor.username(),
                message,
                isPublic,
                details
        ));
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
