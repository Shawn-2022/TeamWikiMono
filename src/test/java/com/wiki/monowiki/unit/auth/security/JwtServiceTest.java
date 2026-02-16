package com.wiki.monowiki.unit.auth.security;

import com.wiki.monowiki.auth.model.Role;
import com.wiki.monowiki.auth.security.JwtProperties;
import com.wiki.monowiki.auth.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void generateToken_and_parse_roundtrip() {
	// 32+ bytes recommended for HS256. Using 64 ASCII chars = 64 bytes.
	String secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	JwtService jwtService = new JwtService(new JwtProperties(secret, 60));

	String token = jwtService.generateToken("admin1", Role.ADMIN);

	var jws = jwtService.parse(token);
	assertThat(jws.getPayload().getSubject()).isEqualTo("admin1");
	assertThat(jws.getPayload().get("role", String.class)).isEqualTo("ADMIN");
	assertThat(jws.getPayload().getExpiration()).isNotNull();
    }

    @Test
    void parse_invalid_token_throws() {
	String secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
	JwtService jwtService = new JwtService(new JwtProperties(secret, 60));

	assertThatThrownBy(() -> jwtService.parse("not-a-jwt"))
		.isInstanceOf(JwtException.class);
    }
}
