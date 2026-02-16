package com.wiki.monowiki.audit.controller;

import com.wiki.monowiki.audit.dto.AuditDtos.AuditEventResponse;
import com.wiki.monowiki.audit.model.*;
import com.wiki.monowiki.audit.service.AuditQueryService;
import com.wiki.monowiki.common.response.BasePageResponse;
import com.wiki.monowiki.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@Tag(name = "Audit", description = "Audit log (ADMIN/EDITOR) and space activity feed (all roles).")
public class AuditController {

    private final AuditQueryService service;

    public AuditController(AuditQueryService service) {
	this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/audit")
    @Operation(summary = "Search audit events (ADMIN/EDITOR)")
    public BasePageResponse<AuditEventResponse> audit(
	    @RequestParam(required = false) String spaceKey,
	    @RequestParam(required = false) Long articleId,
	    @RequestParam(required = false) Long actorId,
	    @RequestParam(required = false) String actor,
	    @RequestParam(required = false) AuditEventType eventType,
	    @RequestParam(required = false) AuditEntityType entityType,
	    @RequestParam(required = false) Long entityId,
	    @RequestParam(required = false) Instant from,
	    @RequestParam(required = false) Instant to,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "20") int size
    ) {
	Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	return BasePageResponse.fromPage(
		service.search(spaceKey, articleId, actorId, actor, eventType, entityType, entityId, null, from, to, pageable),
		"Audit fetched"
	);
    }

    @GetMapping("/spaces/{spaceKey}/activity")
    @Operation(summary = "Recent activity for a space (all roles)", description = "VIEWERs only see public audit events.")
    public BasePageResponse<AuditEventResponse> recentActivity(
	    @PathVariable String spaceKey,
	    @RequestParam(defaultValue = "0") int page,
	    @RequestParam(defaultValue = "20") int size
    ) {
	Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	Boolean publicOnly = SecurityUtils.isViewer() ? Boolean.TRUE : null;
	return BasePageResponse.fromPage(
		service.search(spaceKey, null, null, null, null, null, null, publicOnly, null, null, pageable),
		"Recent activity fetched"
	);
    }
}
