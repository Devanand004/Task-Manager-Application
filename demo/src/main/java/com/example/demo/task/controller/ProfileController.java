package com.example.demo.task.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.task.dto.PasswordChangeRequestDTO;
import com.example.demo.task.dto.UserProfileRequestDTO;
import com.example.demo.task.dto.UserProfileResponseDTO;
import com.example.demo.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskService taskService;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder, TaskService taskService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getProfile(@AuthenticationPrincipal User user) {
        UserProfileResponseDTO stats = taskService.getUserStats(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", stats));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileRequestDTO request) {
        
        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Username is already taken"));
        }

        user.setUsername(request.getUsername());
        User saved = userRepository.save(user);
        
        UserProfileResponseDTO stats = taskService.getUserStats(saved);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully. Note: you might need to re-login if username changed.", stats));
    }

    @PutMapping("/password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordChangeRequestDTO request) {
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Invalid current password"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully"));
    }
}
