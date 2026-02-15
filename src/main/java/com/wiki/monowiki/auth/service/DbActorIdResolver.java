package com.wiki.monowiki.auth.service;

import com.wiki.monowiki.audit.service.ActorIdResolver;
import com.wiki.monowiki.auth.model.Users;
import com.wiki.monowiki.auth.repo.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Monolith implementation of {@link ActorIdResolver}.
 * This lives in the Auth module because it reads from the users table.
 * When splitting into microservices, Audit can swap this implementation
 * (e.g. call auth service, or rely on JWT claims only).
 */
@Component
public class DbActorIdResolver implements ActorIdResolver {

    private final UserRepository users;

    public DbActorIdResolver(UserRepository users) {
	this.users = users;
    }

    @Override
    public Long resolve(String username) {
	if (username == null || username.isBlank() || "system".equalsIgnoreCase(username.trim())) {
	    return null;
	}
	return users.findByUsername(username.trim())
		.map(Users::getId)
		.orElse(null);
    }
}
