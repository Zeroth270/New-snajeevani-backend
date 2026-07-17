package com.anvesha.core;

import com.anvesha.core.dto.auth.AuthResponse;
import com.anvesha.core.dto.auth.LoginRequest;
import com.anvesha.core.dto.auth.RegisterRequest;
import com.anvesha.core.entity.Institution;
import com.anvesha.core.repository.InstitutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired InstitutionRepository institutionRepository;

    private Long institutionId;

    @BeforeEach
    void setUp() {
        Institution institution = institutionRepository.save(
                Institution.builder()
                        .name("IIT Bombay")
                        .type("IIT")
                        .ttoContactEmail("tto@iitb.ac.in")
                        .build());
        institutionId = institution.getId();
    }

    @Test
    void register_then_login_returns_valid_jwt() throws Exception {
        // ── Register ──────────────────────────────────────────────────────
        RegisterRequest reg = new RegisterRequest(
                "Dr. Test User", "researcher@iitb.ac.in",
                "password123", institutionId, "RESEARCHER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("RESEARCHER"));

        // ── Login ─────────────────────────────────────────────────────────
        LoginRequest login = new LoginRequest("researcher@iitb.ac.in", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.email()).isEqualTo("researcher@iitb.ac.in");
        assertThat(response.role()).isEqualTo("RESEARCHER");
        assertThat(response.institutionId()).isEqualTo(institutionId);
    }

    @Test
    void login_with_wrong_password_returns_401() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest(
                "Dr. Badpass", "badpass@iitb.ac.in",
                "correct_password", institutionId, "RESEARCHER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Attempt login with wrong password
        LoginRequest bad = new LoginRequest("badpass@iitb.ac.in", "wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicate_email_registration_returns_400() throws Exception {
        RegisterRequest reg = new RegisterRequest(
                "Dr. Dup", "duplicate@iitb.ac.in",
                "password123", institutionId, "RESEARCHER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email already in use")));
    }

    @Test
    void protected_endpoint_without_jwt_returns_403_or_401() throws Exception {
        mockMvc.perform(post("/api/papers"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
