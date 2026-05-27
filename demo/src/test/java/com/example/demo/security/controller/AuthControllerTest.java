package com.example.demo.security.controller;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController — validates registration and login flows.
 *
 * WHY this matters:
 *   Authentication is the security gateway to the entire application.
 *   These tests permanently protect against: privilege escalation during registration,
 *   invalid credential acceptance, missing validation, and broken JWT issuance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ============================================================
    // Registration Tests
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("Registers successfully with valid credentials and returns JWT")
        void register_ValidCredentials_ReturnsTokenAndOk() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"newuser\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.username").value("newuser"));

            User user = userRepository.findByUsername("newuser").orElse(null);
            assertNotNull(user);
            assertEquals(Role.ROLE_USER, user.getRole());
        }

        @Test
        @DisplayName("Prevents privilege escalation — always assigns ROLE_USER even if ADMIN requested")
        void register_RequestAdminRole_AssignsRoleUser() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"wannabeadmin\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk());

            User user = userRepository.findByUsername("wannabeadmin").orElse(null);
            assertNotNull(user);
            assertEquals(Role.ROLE_USER, user.getRole(),
                    "Privilege escalation must be prevented: requesting ADMIN must still register as ROLE_USER");
        }

        @Test
        @DisplayName("Returns 400 when username is already taken")
        void register_DuplicateUsername_ReturnsBadRequest() throws Exception {
            // Pre-create the user
            User existing = new User();
            existing.setUsername("existinguser");
            existing.setPassword(passwordEncoder.encode("password123"));
            existing.setRole(Role.ROLE_USER);
            userRepository.save(existing);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"existinguser\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Returns 400 when username is blank")
        void register_BlankUsername_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 when password is blank")
        void register_BlankPassword_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"validuser\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 when request body is completely missing")
        void register_EmptyBody_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // Login Tests
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @BeforeEach
        void createUser() {
            User user = new User();
            user.setUsername("loginuser");
            user.setPassword(passwordEncoder.encode("correctpassword"));
            user.setRole(Role.ROLE_USER);
            userRepository.save(user);
        }

        @Test
        @DisplayName("Returns JWT token when credentials are correct")
        void login_ValidCredentials_ReturnsToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"loginuser\",\"password\":\"correctpassword\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.username").value("loginuser"));
        }

        @Test
        @DisplayName("Returns 401 when password is incorrect")
        void login_WrongPassword_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"loginuser\",\"password\":\"WRONGPASSWORD\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Returns 401 when user does not exist")
        void login_UnknownUser_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"ghost\",\"password\":\"password123\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Returns 400 when login body is blank")
        void login_BlankFields_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
