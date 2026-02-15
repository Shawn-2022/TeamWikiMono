package com.wiki.monowiki.audit.service;

import com.wiki.monowiki.common.security.SecurityUtils;

public final class AuditActor {
    private AuditActor() {}

    public static String username() {
	return SecurityUtils.username();
    }
}
