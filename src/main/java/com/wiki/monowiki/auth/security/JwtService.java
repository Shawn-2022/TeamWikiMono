package com.wiki.monowiki.auth.security;

import com.wiki.monowiki.auth.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties props;
    private final Key key;

    public JwtService(JwtProperties props) {
	this.props = props;
	this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Role role) {
	Instant now = Instant.now();
	Instant exp = now.plusSeconds(props.expiryMinutes() * 60);

	return Jwts.builder()
		.setSubject(username)
		.claim("role", role.name())
		.setIssuedAt(Date.from(now))
		.setExpiration(Date.from(exp))
		.signWith(key, SignatureAlgorithm.HS256)
		.compact();
    }

    public Jws<Claims> parse(String token) throws JwtException {
	return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
