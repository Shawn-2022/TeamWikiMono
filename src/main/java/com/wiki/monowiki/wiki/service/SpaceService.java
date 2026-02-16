package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;
import com.wiki.monowiki.audit.service.AuditActor;
import com.wiki.monowiki.audit.service.WikiAuditEvent;
import com.wiki.monowiki.common.security.SecurityUtils;
import com.wiki.monowiki.wiki.dto.SpaceDtos.CreateSpaceRequest;
import com.wiki.monowiki.wiki.dto.SpaceDtos.SpaceResponse;
import com.wiki.monowiki.wiki.model.Space;
import com.wiki.monowiki.wiki.repository.SpaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
public class SpaceService {

    private final SpaceRepository repo;
    private final ApplicationEventPublisher publisher;

    public SpaceService(SpaceRepository repo, ApplicationEventPublisher publisher) {
	this.repo = repo;
	this.publisher = publisher;
    }

    @Transactional
    public SpaceResponse create(CreateSpaceRequest req) {
        log.info("Creating space with key='{}' and name='{}' by user={}", req.spaceKey(), req.name(), SecurityUtils.username());
        if (Objects.isNull(req.spaceKey()) || req.spaceKey().isBlank()) {
            log.warn("Space key is null or blank, creation blocked");
            throw new IllegalArgumentException("Space key cannot be null or blank");
        }
        if (repo.existsBySpaceKey(req.spaceKey())) {
            log.warn("Space key '{}' already exists, creation blocked", req.spaceKey());
            throw new IllegalArgumentException("Space key already exists");
        }
        Space s = Space.builder()
                .spaceKey(req.spaceKey().trim())
                .name(req.name().trim())
                .build();
        s = repo.save(s);

        log.info("Space created: id={}, key='{}', name='{}'", s.getId(), s.getSpaceKey(), s.getName());

        publisher.publishEvent(new WikiAuditEvent(
                AuditEventType.SPACE_CREATED,
                AuditEntityType.SPACE,
                s.getId(),
                s.getSpaceKey(),
                null,
                AuditActor.username(),
                "Created space: " + s.getName(),
                true,
                java.util.Map.of("spaceKey", s.getSpaceKey(), "name", s.getName())
        ));

        return new SpaceResponse(s.getId(), s.getSpaceKey(), s.getName());
    }

    @Transactional(readOnly = true)
    public Page<SpaceResponse> list(Pageable pageable) {
        log.info("Listing spaces with pageable={} by user={}", pageable, SecurityUtils.username());
	return repo.findAll(pageable)
		.map(s -> new SpaceResponse(s.getId(), s.getSpaceKey(), s.getName()));
    }

    @Transactional(readOnly = true)
    public SpaceResponse getByKey(String key) {
        log.info("Getting space by key='{}' by user={}", key, SecurityUtils.username());
        if (Objects.isNull(key) || key.isBlank()) {
            log.warn("Space key is null or blank");
            throw new NotFoundException("Space key cannot be null or blank");
        }
        var s = repo.findBySpaceKey(key).orElseThrow(() -> {
            log.warn("Space not found for key='{}'", key);
            return new NotFoundException("Space not found");
        });
	return new SpaceResponse(s.getId(), s.getSpaceKey(), s.getName());
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
