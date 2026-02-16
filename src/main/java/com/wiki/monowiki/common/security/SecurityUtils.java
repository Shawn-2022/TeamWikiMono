package com.wiki.monowiki.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    private static boolean hasRole(String role) {
	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	if (Objects.isNull(auth)) {
	    return false;
	}

	return auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_" + role));
    }

    public static boolean isViewer() {
	return hasRole("VIEWER");
    }

    public static boolean isEditor() {
	return hasRole("EDITOR");
    }

    public static boolean isAdmin() {
	return hasRole("ADMIN");
    }

    public static String username() {
	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	return auth != null ? auth.getName() : null;
    }
}
