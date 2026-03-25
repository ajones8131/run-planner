package com.runplanner.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.auth.dto.AuthResponse;
import com.runplanner.auth.dto.LoginRequest;
import com.runplanner.auth.dto.RefreshRequest;
import com.runplanner.auth.dto.RegisterRequest;
import com.runplanner.config.JwtAuthFilter;
import com.runplanner.config.SecurityConfig;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean com.runplanner.user.UserRepository userRepository;

    @Test
    void register_returns201WithTokens() throws Exception {
        when(authService.register(any()))
            .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("user@example.com", "password123", "Alice"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void register_returns409OnDuplicateEmail() throws Exception {
        when(authService.register(any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("dup@example.com", "password123", null))))
            .andExpect(status().isConflict());
    }

    @Test
    void login_returns200WithTokens() throws Exception {
        when(authService.login(any()))
            .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("user@example.com", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_returns401ForBadCredentials() throws Exception {
        when(authService.login(any()))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("x@x.com", "wrong"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns200WithNewTokens() throws Exception {
        when(authService.refresh(any()))
            .thenReturn(new AuthResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest("old-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void logout_returns204() throws Exception {
        var user = User.builder().id(UUID.randomUUID()).email("u@x.com").passwordHash("h").build();
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user(user)))
            .andExpect(status().isNoContent());
    }
}
