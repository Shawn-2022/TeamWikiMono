package com.wiki.monowiki.wiki.dto;

import com.wiki.monowiki.wiki.model.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class ArticleDtos {

    public record CreateArticleRequest(
	    @NotBlank @Size(max = 200) String title,
	    @NotBlank @Size(min = 1, max = 200000) String content
    ) {}

    public record UpdateTitleRequest(
	    @NotBlank @Size(max = 200) String title
    ) {}

    public record TagSummary(Long id, String name) {}

    public record ArticleResponse(
	    Long id,
	    String spaceKey,
	    String slug,
	    String title,
	    ArticleStatus status,
	    Integer currentVersionNo,
	    String latestContent,
	    List<TagSummary> tags,
	    long currentVersionCommentCount,
	    String createdBy,
	    Instant createdAt,
	    Instant updatedAt
    ) {}
}
