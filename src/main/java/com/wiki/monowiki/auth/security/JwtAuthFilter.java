package com.wiki.monowiki.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
	this.jwtService = jwtService;
	this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
	    throws ServletException, IOException {

	String auth = request.getHeader("Authorization");
	if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
	    String token = auth.substring(7);
	    try {
		Jws<Claims> jws = jwtService.parse(token);
		String username = jws.getBody().getSubject();

		var userDetails = userDetailsService.loadUserByUsername(username);
		var authentication = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);
	    } catch (JwtException | AuthenticationException ex) {
		// Invalid token or user => clear context; continue to security entry point (401)
		SecurityContextHolder.clearContext();
	    }
	}

	chain.doFilter(request, response);
    }
}
