package com.wiki.monowiki.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SpaceDtos {

    public record CreateSpaceRequest(@NotBlank @Size(max = 64) String spaceKey, @NotBlank @Size(max = 120) String name) {
    }

    public record SpaceResponse(Long id, String spaceKey, String name) {
    }
}
