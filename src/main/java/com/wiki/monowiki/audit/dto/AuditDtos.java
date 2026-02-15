package com.wiki.monowiki.audit.dto;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;

import java.time.Instant;

public class AuditDtos {

    public record AuditEventResponse(
	    Long id,
	    AuditEventType eventType,
	    AuditEntityType entityType,
	    Long entityId,
	    String spaceKey,
	    Long articleId,
	    Long actorId,
	    String actor,
	    String message,
	    String metaJson,
	    Instant createdAt
    ) {}
}
