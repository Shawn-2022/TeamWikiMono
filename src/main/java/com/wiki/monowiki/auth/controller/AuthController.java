package com.wiki.monowiki.auth.controller;

import com.wiki.monowiki.auth.dto.AuthDtos.LoginRequest;
import com.wiki.monowiki.auth.dto.AuthDtos.LoginResponse;
import com.wiki.monowiki.auth.repo.UserRepository;
import com.wiki.monowiki.auth.security.JwtService;
import com.wiki.monowiki.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "JWT login. Use the returned token in Swagger's Authorize button.")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository users;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserRepository users) {
	this.authManager = authManager;
	this.jwtService = jwtService;
	this.users = users;
    }

    /**
     * Override global BearerAuth in OpenAPI so Swagger UI clearly shows this endpoint as public.
     */
    @Operation(
	    security = {},
	    summary = "Login (public)",
	    description = "Authenticate with username/password and receive a JWT token."
    )
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
	authManager.authenticate(
		new UsernamePasswordAuthenticationToken(req.username(), req.password())
	);

	var user = users.findByUsername(req.username()).orElseThrow();
	String token = jwtService.generateToken(user.getUsername(), user.getRole());

	return new BaseResponse<>(
		HttpStatus.OK.value(),
		"Login successful",
		false,
		new LoginResponse(token, user.getRole().name())
	);
    }
}
