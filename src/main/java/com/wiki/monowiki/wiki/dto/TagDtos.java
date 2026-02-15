package com.wiki.monowiki.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TagDtos {

    public record CreateTagRequest(
	    @NotBlank @Size(max = 80) String name
    ) {}

    public record TagResponse(Long id, String name) {}

    public record AddTagToArticleRequest(
	    @NotNull Long tagId
    ) {}
}
