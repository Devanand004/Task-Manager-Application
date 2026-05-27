package com.example.demo.security.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.security.dto.AuthResponse;
import com.example.demo.security.dto.LoginRequest;
import com.example.demo.security.dto.RegisterRequest;
import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.security.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Username is already taken"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Enforce ROLE_USER on all registrations to prevent privilege escalation
        user.setRole(Role.ROLE_USER);

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        AuthResponse authResponse = new AuthResponse(token, user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.getUsername()));
        String token = jwtService.generateToken(user);
        
        AuthResponse authResponse = new AuthResponse(token, user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new ApiResponse<>(true, "User logged in successfully", authResponse));
    }
}
