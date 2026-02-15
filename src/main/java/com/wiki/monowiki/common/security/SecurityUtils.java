package com.wiki.monowiki.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

/**
 * Small helper to keep auth/role checks consistent across modules.
 * (Useful now in the monolith, and later when splitting into microservices.)
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String username() {
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	return (a != null) ? a.getName() : "system";
    }

    public static boolean hasRole(String role) {
	if (role == null || role.isBlank()) return false;
	String expected = "ROLE_" + role.trim().toUpperCase();
	Authentication a = SecurityContextHolder.getContext().getAuthentication();
	if (a == null) return false;
	return a.getAuthorities().stream().anyMatch(g -> Objects.equals(g.getAuthority(), expected));
    }

    public static boolean isViewer() {
	return hasRole("VIEWER");
    }
}
