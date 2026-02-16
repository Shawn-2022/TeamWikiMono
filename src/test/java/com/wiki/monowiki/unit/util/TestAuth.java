package com.wiki.monowiki.unit.util;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.stream.Stream;

/**
 * Small helper for unit tests that touch SecurityContextHolder.
 */
public final class TestAuth {

    private TestAuth() {}

    public static void setAuth(String username, String... roles) {
	var authorities = Stream.of(roles)
		.map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
		.map(SimpleGrantedAuthority::new)
		.toList();

	var auth = new TestingAuthenticationToken(username, "N/A", authorities);
	auth.setAuthenticated(true);
	SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clear() {
	SecurityContextHolder.clearContext();
    }
}
