package com.runplanner.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Base64-encoded 256-bit key for tests
        ReflectionTestUtils.setField(jwtService, "secret",
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirySeconds", 3600L);
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
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirySeconds", -1L);
        String token = jwtService.generateAccessToken(UUID.randomUUID());
        assertThat(jwtService.isValid(token)).isFalse();
    }
}
