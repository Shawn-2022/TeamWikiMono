package com.wiki.monowiki.audit.service;

import com.wiki.monowiki.audit.dto.AuditDtos.AuditEventResponse;
import com.wiki.monowiki.audit.model.*;
import com.wiki.monowiki.audit.repo.AuditEventLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditQueryService {

    private final AuditEventLogRepository repo;

    public AuditQueryService(AuditEventLogRepository repo) {
	this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(
	    String spaceKey,
	    Long articleId,
	    Long actorId,
	    String actor,
	    AuditEventType eventType,
	    AuditEntityType entityType,
	    Long entityId,
	    Boolean publicOnly,
	    Instant from,
	    Instant to,
	    Pageable pageable
    ) {
	Specification<AuditEventLog> spec = (root, query, cb) -> {
	    List<Predicate> p = new ArrayList<>();

	    if (spaceKey != null && !spaceKey.isBlank()) p.add(cb.equal(root.get("spaceKey"), spaceKey.trim()));
	    if (articleId != null) p.add(cb.equal(root.get("articleId"), articleId));
	    if (actorId != null) p.add(cb.equal(root.get("actorId"), actorId));
	    if (actor != null && !actor.isBlank()) p.add(cb.equal(root.get("actor"), actor.trim()));
	    if (eventType != null) p.add(cb.equal(root.get("eventType"), eventType));
	    if (entityType != null) p.add(cb.equal(root.get("entityType"), entityType));
	    if (entityId != null) p.add(cb.equal(root.get("entityId"), entityId));
	    if (publicOnly != null) p.add(cb.equal(root.get("publicEvent"), publicOnly));
	    if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
	    if (to != null) p.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));

	    return cb.and(p.toArray(new Predicate[0]));
	};

	Page<AuditEventLog> page = repo.findAll(spec, pageable);
	return page.map(this::toDto);
    }

    private AuditEventResponse toDto(AuditEventLog a) {
	return new AuditEventResponse(
		a.getId(),
		a.getEventType(),
		a.getEntityType(),
		a.getEntityId(),
		a.getSpaceKey(),
		a.getArticleId(),
		a.getActorId(),
		a.getActor(),
		a.getMessage(),
		a.getMetaJson(),
		a.getCreatedAt()
	);
    }
}
