package com.wiki.monowiki.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.monowiki.common.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ensures Spring Security 401 responses follow the assignment's standardized response wrapper.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
	this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
	    @NonNull HttpServletRequest request,
	    HttpServletResponse response,
	    @NonNull AuthenticationException authException
    ) throws IOException {
	if (response.isCommitted()) {
	    return;
	}

	response.setStatus(HttpStatus.UNAUTHORIZED.value());
	response.setCharacterEncoding(StandardCharsets.UTF_8.name());
	response.setContentType(MediaType.APPLICATION_JSON_VALUE);

	var body = new BaseResponse<>(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", true, null);
	objectMapper.writeValue(response.getWriter(), body);
    }
}
