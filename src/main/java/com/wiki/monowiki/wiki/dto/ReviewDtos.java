package com.wiki.monowiki.wiki.dto;

import com.wiki.monowiki.wiki.model.ArticleStatus;
import com.wiki.monowiki.wiki.model.ReviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ReviewDtos {
    public record ReviewRequestResponse(
	    Long id,
	    Long articleId,
	    String articleSlug,
	    String spaceKey,
	    ArticleStatus articleStatus,
	    ReviewStatus status,
	    String requestedBy,
	    Instant requestedAt,
	    String reviewedBy,
	    Instant reviewedAt,
	    String reason
    ) {}

    public record RejectRequest(
	    @NotBlank @Size(max = 500) String reason
    ) {}
}
