package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.wiki.dto.ArticleDtos.*;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleTag;
import com.wiki.monowiki.wiki.model.ArticleVersion;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleTagRepository;
import com.wiki.monowiki.wiki.repo.ArticleVersionRepository;
import com.wiki.monowiki.wiki.repo.VersionCommentRepository;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class SearchService {

    private final ArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleTagRepository articleTags;
    private final VersionCommentRepository comments;

    public SearchService(ArticleRepository articles,
	    ArticleVersionRepository versions,
	    ArticleTagRepository articleTags,
	    VersionCommentRepository comments) {
	this.articles = articles;
	this.versions = versions;
	this.articleTags = articleTags;
	this.comments = comments;
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> search(String spaceKey, String q, Pageable pageable) {
	Page<Article> page = isViewer()
		? articles.searchLatestPublishedInSpace(spaceKey, q, pageable)
		: articles.searchLatestInSpace(spaceKey, q, pageable);

	return page.map(this::toResponse);
    }

    private ArticleResponse toResponse(Article a) {
	Integer cv = a.getCurrentVersionNo();

	String latestContent = null;
	if (cv != null && cv > 0) {
	    latestContent = versions.findByArticleAndVersionNo(a, cv)
		    .map(ArticleVersion::getContent)
		    .orElse(null);
	}

	List<TagSummary> tagSummaries = articleTags.findByArticle(a).stream()
		.map(ArticleTag::getTag)
		.map(t -> new TagSummary(t.getId(), t.getName()))
		.toList();

	long commentCount = (cv != null && cv > 0)
		? comments.countByArticleAndVersionNo(a, cv)
		: 0L;

	return new ArticleResponse(
		a.getId(),
		a.getSpace().getSpaceKey(),
		a.getSlug(),
		a.getTitle(),
		a.getStatus(),
		a.getCurrentVersionNo(),
		latestContent,
		tagSummaries,
		commentCount,
		a.getCreatedBy(),
		a.getCreatedAt(),
		a.getUpdatedAt()
	);
    }

    private boolean isViewer() {
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	if (a == null) return false;
	return a.getAuthorities().stream().anyMatch(g -> Objects.equals(g.getAuthority(), "ROLE_VIEWER"));
    }
}