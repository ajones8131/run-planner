package com.runplanner.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final String secret;
    private final long accessTokenExpirySeconds;
    private SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-expiry-seconds:3600}") long accessTokenExpirySeconds) {
        this.secret = secret;
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
    }

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .issuer("run-planner")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpirySeconds * 1_000))
            .signWith(signingKey)
            .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer("run-planner")
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
