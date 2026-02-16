package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.AuditActor;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.TagDtos.CreateTagRequest;
import com.wiki.monowiki.wiki.dto.TagDtos.TagResponse;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ArticleTag;
import com.wiki.monowiki.wiki.model.Tag;
import com.wiki.monowiki.wiki.repository.ArticleRepository;
import com.wiki.monowiki.wiki.repository.ArticleTagRepository;
import com.wiki.monowiki.wiki.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class TagService {

    private final TagRepository tags;
    private final ArticleRepository articles;
    private final ArticleTagRepository articleTags;
    private final ApplicationEventPublisher publisher;

    public TagService(TagRepository tags, ArticleRepository articles, ArticleTagRepository articleTags, ApplicationEventPublisher publisher) {
	this.tags = tags;
	this.articles = articles;
	this.articleTags = articleTags;
	this.publisher = publisher;
    }

    @Transactional
    public TagResponse create(CreateTagRequest req) {
        String name = req.name() != null ? req.name().trim() : "";
        log.info("Creating tag with name='{}' by user={}", name, currentUsername());
        if (Objects.isNull(req.name()) || name.isBlank()) {
            log.warn("Tag creation blocked: tag name is null or blank");
            throw new IllegalArgumentException("Tag name cannot be null or blank");
        }
        if (tags.existsByNameIgnoreCase(name)) {
            log.warn("Tag creation blocked: tag '{}' already exists", name);
            throw new IllegalArgumentException("Tag already exists");
        }
        try {
            Tag t = tags.save(Tag.builder().name(name).build());
            log.info("Tag created: id={}, name='{}'", t.getId(), t.getName());
            return new TagResponse(t.getId(), t.getName());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Tag creation failed due to race condition: tag '{}' already exists", name);
            throw new IllegalArgumentException("Tag already exists");
        }
    }

    @Transactional(readOnly = true)
    public Page<TagResponse> list(Pageable pageable) {
        log.info("Listing tags with pageable={} by user={}", pageable, currentUsername());
        return tags.findAll(pageable)
            .map(t -> new TagResponse(t.getId(), t.getName()));
    }

    @Transactional
    public void addTagToArticle(Long articleId, Long tagId) {
        log.info("Adding tagId={} to articleId={} by user={}", tagId, articleId, currentUsername());
        Article a = articles.findById(articleId).orElseThrow(() -> {
            log.warn("Article not found for id={} during tag add", articleId);
            return new NotFoundException("Article not found");
        });
        Tag t = tags.findById(tagId).orElseThrow(() -> {
            log.warn("Tag not found for id={} during tag add", tagId);
            return new NotFoundException("Tag not found");
        });

        if (!articleTags.existsByArticleAndTag(a, t)) {
            articleTags.save(ArticleTag.builder().article(a).tag(t).build());
            log.info("Tag '{}' added to articleId={}", t.getName(), articleId);
        } else {
            log.debug("Tag '{}' already present on articleId={}", t.getName(), articleId);
        }

        boolean isPublic = Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED);
        publishTagEvent(
            AuditEventType.TAG_ADDED_TO_ARTICLE,
            t,
            a,
            "Added tag '" + t.getName() + "' to article",
            isPublic,
            Map.of("tagId", t.getId(), "tagName", t.getName())
        );
    }

    @Transactional
    public void removeTagFromArticle(Long articleId, Long tagId) {
        log.info("Removing tagId={} from articleId={} by user={}", tagId, articleId, currentUsername());
        Article a = articles.findById(articleId).orElseThrow(() -> {
            log.warn("Article not found for id={} during tag remove", articleId);
            return new NotFoundException("Article not found");
        });
        Tag t = tags.findById(tagId).orElseThrow(() -> {
            log.warn("Tag not found for id={} during tag remove", tagId);
            return new NotFoundException("Tag not found");
        });

        articleTags.deleteByArticleAndTag(a, t);
        log.info("Tag '{}' removed from articleId={}", t.getName(), articleId);

        boolean isPublic = Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED);
        publishTagEvent(
            AuditEventType.TAG_REMOVED_FROM_ARTICLE,
            t,
            a,
            "Removed tag '" + t.getName() + "' from article",
            isPublic,
            Map.of("tagId", t.getId(), "tagName", t.getName())
        );
    }

    private void publishTagEvent(
            AuditEventType eventType,
            Tag tag,
            Article article,
            String message,
            boolean isPublic,
            Map<String, Object> details
    ) {
        publisher.publishEvent(new WikiAuditEvent(
                eventType,
                AuditEntityType.TAG,
                tag.getId(),
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

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
