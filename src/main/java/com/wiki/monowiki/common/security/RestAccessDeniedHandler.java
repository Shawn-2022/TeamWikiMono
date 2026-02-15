package com.wiki.monowiki.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.monowiki.common.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ensures Spring Security 403 responses follow the assignment's standardized response wrapper.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
	this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
	    @NonNull HttpServletRequest request,
	    HttpServletResponse response,
	    @NonNull AccessDeniedException accessDeniedException
    ) throws IOException {
	if (response.isCommitted()) {
	    return;
	}

	response.setStatus(HttpStatus.FORBIDDEN.value());
	response.setCharacterEncoding(StandardCharsets.UTF_8.name());
	response.setContentType(MediaType.APPLICATION_JSON_VALUE);

	var body = new BaseResponse<>(HttpStatus.FORBIDDEN.value(), "Forbidden", true, null);
	objectMapper.writeValue(response.getWriter(), body);
    }
}
