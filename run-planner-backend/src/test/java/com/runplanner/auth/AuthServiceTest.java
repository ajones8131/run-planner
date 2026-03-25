package com.runplanner.auth;

import com.runplanner.auth.dto.LoginRequest;
import com.runplanner.auth.dto.RegisterRequest;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthService authService;

    @Test
    void register_savesUserWithHashedPassword() {
        var request = new RegisterRequest("user@example.com", "password123", "Alice");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access");
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void register_throwsOnDuplicateEmail() {
        when(userRepository.existsByEmail(any())).thenReturn(true);
        assertThatThrownBy(() -> authService.register(
            new RegisterRequest("dup@example.com", "password123", null)))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_returnsTokensForValidCredentials() {
        var user = User.builder().id(UUID.randomUUID()).email("u@x.com")
            .passwordHash("hashed").build();
        when(userRepository.findByEmail("u@x.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId())).thenReturn("access");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(new LoginRequest("u@x.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_throwsForUnknownEmail() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@x.com", "pw")))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_throwsForWrongPassword() {
        var user = User.builder().id(UUID.randomUUID()).passwordHash("hashed").build();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@x.com", "wrong")))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void refresh_returnsNewTokensForValidRefreshToken() {
        var user = User.builder().id(UUID.randomUUID()).build();
        var stored = RefreshToken.builder()
            .user(user).tokenHash("hashed-token")
            .expiresAt(Instant.now().plusSeconds(3600))
            .revoked(false).build();
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user.getId())).thenReturn("new-access");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.refresh("raw-token", "hashed-token");

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue(); // old token rotated out
    }

    @Test
    void refresh_throwsForRevokedToken() {
        var stored = RefreshToken.builder()
            .tokenHash("h").expiresAt(Instant.now().plusSeconds(3600)).revoked(true).build();
        when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> authService.refresh("raw", "h"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void refresh_throwsForExpiredToken() {
        var stored = RefreshToken.builder()
            .tokenHash("h").expiresAt(Instant.now().minusSeconds(1)).revoked(false).build();
        when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(stored));
        assertThatThrownBy(() -> authService.refresh("raw", "h"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void logout_revokesAllRefreshTokens() {
        var user = User.builder().id(UUID.randomUUID()).build();
        authService.logout(user);
        verify(refreshTokenRepository).revokeAllByUser(user);
    }
}
