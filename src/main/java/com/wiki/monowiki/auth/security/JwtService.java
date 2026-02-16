package com.wiki.monowiki.auth.security;

import com.wiki.monowiki.auth.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
	this.props = props;
	this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, Role role) {
	Instant now = Instant.now();
	Instant exp = now.plusSeconds(props.expiryMinutes() * 60L);

	return Jwts.builder()
		.subject(username)
		.claim("role", role.name())
		.issuedAt(Date.from(now))
		.expiration(Date.from(exp))
		.signWith(key, Jwts.SIG.HS256)
		.compact();
    }

    public Jws<Claims> parse(String token) throws JwtException {
	return Jwts.parser()
		.verifyWith(key)
		.build()
		.parseSignedClaims(token);
    }
}
