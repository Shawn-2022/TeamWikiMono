package com.wiki.monowiki.common.exception;

import com.wiki.monowiki.common.response.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Object>> unauthorized(AuthenticationException ex) {
	String msg;
	if (ex instanceof BadCredentialsException || ex instanceof UsernameNotFoundException) {
	    msg = "Invalid username or password";
	} else {
	    msg = "Unauthorized";
	}
	return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
		.body(new BaseResponse<>(HttpStatus.UNAUTHORIZED.value(), msg, true, null));
    }

    @ExceptionHandler({
	    com.wiki.monowiki.wiki.service.SpaceService.NotFoundException.class,
	    com.wiki.monowiki.wiki.service.ArticleService.NotFoundException.class,
	    com.wiki.monowiki.wiki.service.VersionService.NotFoundException.class,
	    com.wiki.monowiki.wiki.service.ReviewService.NotFoundException.class,
	    com.wiki.monowiki.wiki.service.TagService.NotFoundException.class,
	    com.wiki.monowiki.wiki.service.CommentService.NotFoundException.class,
    })
    public ResponseEntity<BaseResponse<Object>> notFound(RuntimeException ex) {
	return ResponseEntity.status(HttpStatus.NOT_FOUND)
		.body(new BaseResponse<>(HttpStatus.NOT_FOUND.value(), ex.getMessage(), true, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Object>> badRequest(RuntimeException ex) {
	return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		.body(new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), true, null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Object>> dataIntegrity(DataIntegrityViolationException ex) {
	String msg = "Data integrity violation";
	Throwable root = ex.getMostSpecificCause();
	if (root.getMessage() != null) {
	    String m = root.getMessage();
	    if (m.contains("uk_space_slug")) {
		msg = "Slug already exists in this space";
	    } else if (m.contains("uk_tag_name") || m.contains("uk_tag_name_ci")) {
		msg = "Tag already exists";
	    }
	}
	return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		.body(new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), msg, true, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> validation(MethodArgumentNotValidException ex) {
	String msg = ex.getBindingResult().getFieldErrors().stream()
		.map(e -> e.getField() + " " + e.getDefaultMessage())
		.findFirst().orElse("Validation error");
	return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		.body(new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), msg, true, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Object>> validation2(ConstraintViolationException ex) {
	return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		.body(new BaseResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), true, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Object>> forbidden(AccessDeniedException ex) {
	return ResponseEntity.status(HttpStatus.FORBIDDEN)
		.body(new BaseResponse<>(HttpStatus.FORBIDDEN.value(), "Forbidden", true, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> unknown(Exception ex) {
	// IMPORTANT: don't leak internal details in interview assignment output
	log.error("Unhandled exception", ex);
	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		.body(new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", true, null));
    }
}
