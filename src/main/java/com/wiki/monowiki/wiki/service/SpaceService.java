package com.wiki.monowiki.wiki.service;

import com.wiki.monowiki.wiki.dto.SpaceDtos.CreateSpaceRequest;
import com.wiki.monowiki.wiki.dto.SpaceDtos.SpaceResponse;
import com.wiki.monowiki.wiki.model.Space;
import com.wiki.monowiki.wiki.repo.SpaceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpaceService {

    private final SpaceRepository repo;
    private final ApplicationEventPublisher publisher;

    public SpaceService(SpaceRepository repo, ApplicationEventPublisher publisher) {
	this.repo = repo;
	this.publisher = publisher;
    }

    @Transactional
    public SpaceResponse create(CreateSpaceRequest req) {
	if (repo.existsBySpaceKey(req.spaceKey())) {
	    throw new IllegalArgumentException("Space key already exists");
	}
	Space s = Space.builder()
		.spaceKey(req.spaceKey().trim())
		.name(req.name().trim())
		.build();
	s = repo.save(s);

	publisher.publishEvent(new com.wiki.monowiki.audit.service.WikiAuditEvent(
		com.wiki.monowiki.audit.model.AuditEventType.SPACE_CREATED,
		com.wiki.monowiki.audit.model.AuditEntityType.SPACE,
		s.getId(),
		s.getSpaceKey(),
		null,
		com.wiki.monowiki.audit.service.AuditActor.username(),
		"Created space: " + s.getName(),
		true,
		java.util.Map.of("spaceKey", s.getSpaceKey(), "name", s.getName())
	));

	return new SpaceResponse(s.getId(), s.getSpaceKey(), s.getName());
    }

    @Transactional(readOnly = true)
    public Page<SpaceResponse> list(Pageable pageable) {
	return repo.findAll(pageable)
		.map(s -> new SpaceResponse(s.getId(), s.getSpaceKey(), s.getName()));
    }

    @Transactional(readOnly = true)
    public SpaceResponse getByKey(String key) {
	var s = repo.findBySpaceKey(key).orElseThrow(() -> new NotFoundException("Space not found"));
	return new SpaceResponse(s.getId(), s.getSpaceKey(), s.getName());
    }

    public static class NotFoundException extends RuntimeException {
	public NotFoundException(String msg) { super(msg); }
    }
}
