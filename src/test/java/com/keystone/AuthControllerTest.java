package com.keystone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keystone.model.Role;
import com.keystone.model.User;
import com.keystone.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer auth tests:
 * - Successful login returns JWT + role
 * - Wrong password returns 400/401
 * - Protected endpoint without token returns 401/403
 * - Protected endpoint with valid token succeeds
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc         mockMvc;
    @Autowired ObjectMapper    objectMapper;
    @Autowired UserRepository  userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User testManager;

    @BeforeEach
    void setUp() {
        testManager = new User("Test Manager", "authtest@test.com",
                passwordEncoder.encode("Password123!"), Role.MANAGER);
        userRepository.save(testManager);
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "authtest@test.com", "password", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void login_withWrongPassword_returnsError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "authtest@test.com", "password", "WrongPass!"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_withUnknownEmail_returnsError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody@test.com", "password", "anything"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401or403() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().is(403)); // Spring Security default without filter config is 403
    }

    @Test
    void protectedEndpoint_withValidToken_succeeds() throws Exception {
        // Get token
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "authtest@test.com", "password", "Password123!"))))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        // Use token on protected endpoint
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
