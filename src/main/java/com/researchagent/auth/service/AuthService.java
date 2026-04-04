package com.researchagent.auth.service;

import com.researchagent.auth.dto.AuthResponse;
import com.researchagent.auth.dto.LoginRequest;
import com.researchagent.auth.dto.SignupRequest;
import com.researchagent.auth.entity.AppUser;
import com.researchagent.auth.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        AppUser saved = appUserRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token);
    }
}
