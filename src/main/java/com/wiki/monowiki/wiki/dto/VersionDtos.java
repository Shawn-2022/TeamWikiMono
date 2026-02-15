package com.wiki.monowiki.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class VersionDtos {

    public record CreateVersionRequest(
	    @NotBlank @Size(min = 1, max = 200000) String content
    ) {}

    public record VersionResponse(
	    Long id,
	    Long articleId,
	    Integer versionNo,
	    String content,
	    String createdBy,
	    Instant createdAt
    ) {}
}
