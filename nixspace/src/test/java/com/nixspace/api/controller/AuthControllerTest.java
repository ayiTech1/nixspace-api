package com.nixspace.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nixspace.api.dto.request.AuthRequests.*;
import com.nixspace.api.dto.response.Responses.*;
import com.nixspace.domain.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController (MockMvc)")
class AuthControllerTest {

    @Autowired MockMvc       mvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean AuthService authService;

    private UserResponse dummyUser() {
        return new UserResponse(1L, "alice@example.com", "Alice",
                null, null, "UTC", true, null, Instant.now());
    }

    // ─── POST /api/v1/auth/register ────────────────────────────────────────

    @Test
    @DisplayName("POST /register → 201 with token pair on valid input")
    void register_returns201() throws Exception {
        var req = new RegisterRequest("alice@example.com", "password123", "Alice", "UTC");
        var resp = new AuthResponse("access-token", "refresh-token", 3600, dummyUser());

        when(authService.register(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /register → 400 when email is missing")
    void register_missingEmail_returns400() throws Exception {
        var body = """
                { "password": "password123", "displayName": "Alice" }
                """;

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /register → 400 when password is too short")
    void register_shortPassword_returns400() throws Exception {
        var body = """
                { "email": "alice@test.com", "password": "short", "displayName": "Alice" }
                """;

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/v1/auth/login ───────────────────────────────────────────

    @Test
    @DisplayName("POST /login → 200 with token pair on valid credentials")
    void login_returns200() throws Exception {
        var req  = new LoginRequest("alice@example.com", "password123");
        var resp = new AuthResponse("access-token", "refresh-token", 3600, dummyUser());

        when(authService.login(any(), any(), any())).thenReturn(resp);

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /login → 401 on bad credentials")
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any(), any(), any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "alice@test.com", "password": "wrong" }
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ─── POST /api/v1/auth/refresh ─────────────────────────────────────────

    @Test
    @DisplayName("POST /refresh → 200 with new token pair")
    void refresh_returns200() throws Exception {
        var resp = new AuthResponse("new-access", "new-refresh", 3600, dummyUser());
        when(authService.refreshToken(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "refreshToken": "some-valid-token" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }
}
