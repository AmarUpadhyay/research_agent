package com.researchagent.auth.controller;

import com.researchagent.auth.dto.AuthResponse;
import com.researchagent.auth.dto.LoginRequest;
import com.researchagent.auth.dto.SignupRequest;
import com.researchagent.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(IllegalArgumentException ex) {
        HttpStatus status = "Email already in use".equals(ex.getMessage()) ? HttpStatus.CONFLICT : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
    }
}
