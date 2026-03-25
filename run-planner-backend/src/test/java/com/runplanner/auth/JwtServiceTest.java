package com.runplanner.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Base64-encoded 256-bit key for tests
        String secret = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2";
        jwtService = new JwtService(secret, 3600L);
        jwtService.init();
    }

    @Test
    void generateAccessToken_returnsNonBlankToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUserId_roundTrips() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractUserId_throwsOnInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractUserId("not.a.token"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID());
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        JwtService expiredService = new JwtService("dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2", -1L);
        expiredService.init();
        String token = expiredService.generateAccessToken(UUID.randomUUID());
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForNullToken() {
        assertThat(jwtService.isValid(null)).isFalse();
    }
}
