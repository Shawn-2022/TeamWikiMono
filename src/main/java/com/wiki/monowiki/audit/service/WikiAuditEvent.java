package com.wiki.monowiki.audit.service;

import com.wiki.monowiki.audit.model.AuditEntityType;
import com.wiki.monowiki.audit.model.AuditEventType;

import java.util.Map;

public record WikiAuditEvent(
	AuditEventType eventType,
	AuditEntityType entityType,
	Long entityId,
	String spaceKey,
	Long articleId,
	String actor,
	String message,
	boolean publicEvent,
	Map<String, Object> meta
) {}
