package com.wiki.monowiki.auth.security;

import com.wiki.monowiki.auth.repo.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DbUserDetailsService(UserRepository users) {
	this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
	var u = users.findByUsername(username)
		.orElseThrow(() -> new UsernameNotFoundException("User not found"));

	// Spring expects ROLE_ prefix for hasRole()
	return org.springframework.security.core.userdetails.User
		.withUsername(u.getUsername())
		.password(u.getPasswordHash())
		.authorities(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
		.build();
    }
}