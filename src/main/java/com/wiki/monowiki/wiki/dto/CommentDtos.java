package com.wiki.monowiki.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CommentDtos {

    public record CreateCommentRequest(
	    @NotBlank @Size(max = 2000) String body
    ) {}

    public record CommentResponse(
	    Long id,
	    Long articleId,
	    Integer versionNo,
	    String body,
	    String createdBy,
	    Instant createdAt
    ) {}
}
