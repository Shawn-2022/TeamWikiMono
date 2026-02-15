package com.wiki.monowiki.audit.service;

/**
 * Small seam between Audit and Auth.
 *
 * In the monolith, we resolve actorId from the users table.
 * When splitting into microservices, the audit service can provide a different implementation
 * (e.g., call the auth service or rely purely on claims).
 */
public interface ActorIdResolver {

    /**
     * @param username authenticated username
     * @return internal user id, or null if unknown/not resolvable
     */
    Long resolve(String username);
}
