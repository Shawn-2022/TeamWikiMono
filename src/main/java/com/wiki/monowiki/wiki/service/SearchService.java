package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.ArticleDtos.ArticleResponse;
import com.wiki.monowiki.wiki.dto.ArticleDtos.TagSummary;
import com.wiki.monowiki.wiki.model.Article;
import com.wiki.monowiki.wiki.model.ArticleTag;
import com.wiki.monowiki.wiki.model.ArticleVersion;
import com.wiki.monowiki.wiki.repo.ArticleRepository;
import com.wiki.monowiki.wiki.repo.ArticleTagRepository;
import com.wiki.monowiki.wiki.repo.ArticleVersionRepository;
import com.wiki.monowiki.wiki.repo.VersionCommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
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
    public Page<ArticleResponse> search(String spaceKey, String q, boolean includeArchived, Pageable pageable) {
        log.info("Searching articles in spaceKey='{}', query='{}', includeArchived={}, by user={}", spaceKey, q, includeArchived, SecurityUtils.username());
        Page<Article> page;
        if (SecurityUtils.isViewer()) {
            log.debug("Viewer search: only published articles will be returned for spaceKey='{}'", spaceKey);
            page = articles.searchLatestPublishedInSpace(spaceKey, q, pageable);
        } else if (includeArchived) {
            log.debug("Search including archived articles for spaceKey='{}'", spaceKey);
            page = articles.searchLatestInSpace(spaceKey, q, pageable);
        } else {
            log.debug("Search excluding archived articles for spaceKey='{}'", spaceKey);
            page = articles.searchLatestNonArchivedInSpace(spaceKey, q, pageable);
        }
        log.info("Search result: {} articles found for spaceKey='{}'", page.getTotalElements(), spaceKey);
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
}
