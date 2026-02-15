package com.wiki.monowiki.auth.security;

import com.wiki.monowiki.common.security.RestAccessDeniedHandler;
import com.wiki.monowiki.common.security.RestAuthEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
	    HttpSecurity http,
	    JwtAuthFilter jwtFilter,
	    RestAuthEntryPoint authEntryPoint,
	    RestAccessDeniedHandler accessDeniedHandler
    ) {
	http
		// apply this chain to all endpoints
		.securityMatcher("/**")
		// CSRF OFF (important for POST /auth/login)
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.exceptionHandling(eh -> eh
			.authenticationEntryPoint(authEntryPoint)
			.accessDeniedHandler(accessDeniedHandler)
		)
		.authorizeHttpRequests(auth -> auth
			.requestMatchers(
				"/auth/**",
				"/swagger-ui/**",
				"/swagger-ui.html",
				"/v3/api-docs/**"
			).permitAll()
			.anyRequest().authenticated()
		)
		.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

	return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
	return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) {
	return cfg.getAuthenticationManager();
    }
}
