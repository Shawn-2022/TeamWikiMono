package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.wiki.dto.TagDtos.*;
import com.wiki.monowiki.wiki.model.*;
import com.wiki.monowiki.wiki.repo.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
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
	String name = req.name().trim();
	if (tags.existsByNameIgnoreCase(name)) {
	    throw new IllegalArgumentException("Tag already exists");
	}
	try {
	    Tag t = tags.save(Tag.builder().name(name).build());
	    return new TagResponse(t.getId(), t.getName());
	} catch (DataIntegrityViolationException ex) {
	    // Race-condition safe (DB unique index is the final authority)
	    throw new IllegalArgumentException("Tag already exists");
	}
    }

    @Transactional(readOnly = true)
    public Page<TagResponse> list(Pageable pageable) {
	return tags.findAll(pageable)
		.map(t -> new TagResponse(t.getId(), t.getName()));
    }

    @Transactional
    public void addTagToArticle(Long articleId, Long tagId) {
	Article a = articles.findById(articleId).orElseThrow(() -> new NotFoundException("Article not found"));
	Tag t = tags.findById(tagId).orElseThrow(() -> new NotFoundException("Tag not found"));

	if (!articleTags.existsByArticleAndTag(a, t)) {
	    articleTags.save(ArticleTag.builder().article(a).tag(t).build());
	}

	boolean isPublic = a.getStatus() == ArticleStatus.PUBLISHED;

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.TAG_ADDED_TO_ARTICLE,
		com.wiki.monowiki.audit.model.AuditEntityType.TAG,
		t.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Added tag '" + t.getName() + "' to article",
		isPublic,
		Map.of("tagId", t.getId(), "tagName", t.getName())
	));
    }

    @Transactional
    public void removeTagFromArticle(Long articleId, Long tagId) {
	Article a = articles.findById(articleId).orElseThrow(() -> new NotFoundException("Article not found"));
	Tag t = tags.findById(tagId).orElseThrow(() -> new NotFoundException("Tag not found"));

	articleTags.deleteByArticleAndTag(a, t);

	boolean isPublic = a.getStatus() == ArticleStatus.PUBLISHED;

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.TAG_REMOVED_FROM_ARTICLE,
		com.wiki.monowiki.audit.model.AuditEntityType.TAG,
		t.getId(),
		a.getSpace().getSpaceKey(),
		a.getId(),
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Removed tag '" + t.getName() + "' from article",
		isPublic,
		Map.of("tagId", t.getId(), "tagName", t.getName())
	));
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
