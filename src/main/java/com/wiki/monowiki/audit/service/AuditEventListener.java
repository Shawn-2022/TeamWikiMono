package com.wiki.monowiki.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.monowiki.audit.model.AuditEventLog;
import com.wiki.monowiki.audit.repo.AuditEventLogRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Objects;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
public class AuditEventListener {

    private final AuditEventLogRepository repo;
    private final ObjectMapper objectMapper;
    private final @Nullable ActorIdResolver actorIdResolver;

    public AuditEventListener(
	    AuditEventLogRepository repo,
	    ObjectMapper objectMapper,
	    @Nullable ActorIdResolver actorIdResolver
    ) {
	this.repo = repo;
	this.objectMapper = objectMapper;
	this.actorIdResolver = actorIdResolver;
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onEvent(WikiAuditEvent e) {
	String metaJson = toJsonQuietly(e.meta());
	Long actorId = (actorIdResolver != null)
		? actorIdResolver.resolve(e.actor())
		: null;

	repo.save(AuditEventLog.builder()
		.eventType(e.eventType())
		.entityType(e.entityType())
		.entityId(e.entityId())
		.spaceKey(e.spaceKey())
		.articleId(e.articleId())
		.actorId(actorId)
		.actor(e.actor())
		.message(e.message())
		.publicEvent(e.publicEvent())
		.metaJson(metaJson)
		.build());
    }

    private String toJsonQuietly(Map<String, Object> meta) {
	if (Objects.isNull(meta) || meta.isEmpty()) return null;
	try {
	    return objectMapper.writeValueAsString(meta);
	} catch (Exception ex) {
	    return null; // audit must never fail core flow
	}
    }
}
