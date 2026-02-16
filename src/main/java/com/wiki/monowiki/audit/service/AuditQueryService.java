package com.wiki.monowiki.audit.service;

import com.wiki.monowiki.audit.dto.AuditDtos.AuditEventResponse;
import com.wiki.monowiki.audit.model.*;
import com.wiki.monowiki.audit.repo.AuditEventLogRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
	Specification<AuditEventLog> spec =
		(root, query, cb) -> cb.and(buildPredicates(
			root, cb,
			spaceKey, articleId, actorId, actor,
			eventType, entityType, entityId,
			publicOnly, from, to
		));

	return repo.findAll(spec, pageable).map(this::toDto);
    }
    private Predicate[] buildPredicates(
	    Root<AuditEventLog> root,
	    CriteriaBuilder cb,
	    String spaceKey,
	    Long articleId,
	    Long actorId,
	    String actor,
	    AuditEventType eventType,
	    AuditEntityType entityType,
	    Long entityId,
	    Boolean publicOnly,
	    Instant from,
	    Instant to
    ) {
	List<Predicate> p = new ArrayList<>();

	addEquals(p, cb, root.get("spaceKey"), spaceKey);
	addEquals(p, cb, root.get("articleId"), articleId);
	addEquals(p, cb, root.get("actorId"), actorId);
	addEquals(p, cb, root.get("actor"), trim(actor));
	addEquals(p, cb, root.get("eventType"), eventType);
	addEquals(p, cb, root.get("entityType"), entityType);
	addEquals(p, cb, root.get("entityId"), entityId);
	addEquals(p, cb, root.get("publicEvent"), publicOnly);

	addFrom(p, cb, root.get("createdAt"), from);
	addTo(p, cb, root.get("createdAt"), to);

	return p.toArray(new Predicate[0]);
    }
    private <T> void addEquals(List<Predicate> p, CriteriaBuilder cb, Path<T> path, T value) {
	if (value != null) {
	    p.add(cb.equal(path, value));
	}
    }

    private void addFrom(List<Predicate> p, CriteriaBuilder cb, Path<Instant> path, Instant from) {
	if (from != null) {
	    p.add(cb.greaterThanOrEqualTo(path, from));
	}
    }

    private void addTo(List<Predicate> p, CriteriaBuilder cb, Path<Instant> path, Instant to) {
	if (to != null) {
	    p.add(cb.lessThanOrEqualTo(path, to));
	}
    }

    private String trim(String value) {
	return (value == null || value.isBlank()) ? null : value.trim();
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
