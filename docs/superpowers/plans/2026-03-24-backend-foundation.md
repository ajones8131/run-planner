# Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap the Spring Boot monolith with JWT authentication, user profile management, and goal race CRUD — the complete backend foundation that all other plans depend on.

**Architecture:** Single Spring Boot 3.4 monolith (Java 25) with PostgreSQL managed by Flyway migrations. JWT access tokens (1hr TTL) paired with rotated refresh tokens (30-day TTL) stored in the database. Service classes own all business logic; controllers are thin request/response adapters. All tests are pure unit tests using Mockito or `@WebMvcTest` — no embedded database needed.

**Tech Stack:** Java 25, Spring Boot 3.4, Spring Security 6, Spring Data JPA, PostgreSQL 16, Flyway 10, jjwt 0.12, Lombok, JUnit 5, Mockito

---

## File Map

```
run-planner-backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/runplanner/
    │   │   ├── RunPlannerApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── JwtAuthFilter.java
    │   │   ├── user/
    │   │   │   ├── Units.java
    │   │   │   ├── User.java
    │   │   │   ├── UserRepository.java
    │   │   │   ├── UserService.java
    │   │   │   ├── UserController.java
    │   │   │   └── dto/
    │   │   │       ├── UpdateProfileRequest.java
    │   │   │       └── UserResponse.java
    │   │   ├── auth/
    │   │   │   ├── RefreshToken.java
    │   │   │   ├── RefreshTokenRepository.java
    │   │   │   ├── AuthService.java
    │   │   │   ├── JwtService.java
    │   │   │   ├── AuthController.java
    │   │   │   └── dto/
    │   │   │       ├── RegisterRequest.java
    │   │   │       ├── LoginRequest.java
    │   │   │       ├── RefreshRequest.java
    │   │   │       └── AuthResponse.java
    │   │   └── goalrace/
    │   │       ├── GoalRaceStatus.java
    │   │       ├── GoalRace.java
    │   │       ├── GoalRaceRepository.java
    │   │       ├── GoalRaceService.java
    │   │       ├── GoalRaceController.java
    │   │       └── dto/
    │   │           ├── CreateGoalRaceRequest.java
    │   │           ├── UpdateGoalRaceRequest.java
    │   │           └── GoalRaceResponse.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           ├── V1__create_users.sql
    │           ├── V2__create_refresh_tokens.sql
    │           └── V3__create_goal_races.sql
    └── test/
        └── java/com/runplanner/
            ├── auth/
            │   ├── JwtServiceTest.java
            │   ├── AuthServiceTest.java
            │   └── AuthControllerTest.java
            ├── user/
            │   └── UserControllerTest.java
            └── goalrace/
                ├── GoalRaceServiceTest.java
                └── GoalRaceControllerTest.java
```

---

## Task 1: Bootstrap the Maven project

**Files:**
- Create: `run-planner-backend/pom.xml`
- Create: `run-planner-backend/src/main/java/com/runplanner/RunPlannerApplication.java`
- Create: `run-planner-backend/src/main/resources/application.properties`

- [ ] **Step 1: Create the directory structure**

```bash
mkdir -p run-planner-backend/src/main/java/com/runplanner
mkdir -p run-planner-backend/src/main/resources/db/migration
mkdir -p run-planner-backend/src/test/java/com/runplanner
```

- [ ] **Step 2: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
    </parent>

    <groupId>com.runplanner</groupId>
    <artifactId>run-planner-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>run-planner-backend</name>

    <properties>
        <java.version>25</java.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create `RunPlannerApplication.java`**

```java
package com.runplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RunPlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunPlannerApplication.class, args);
    }
}
```

- [ ] **Step 4: Create `application.properties`**

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/runplanner
spring.datasource.username=runplanner
spring.datasource.password=runplanner
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT
jwt.secret=CHANGE_ME_generate_a_base64_encoded_256bit_key
jwt.access-token-expiry-seconds=3600
jwt.refresh-token-expiry-days=30
```

- [ ] **Step 5: Verify the project compiles**

```bash
cd run-planner-backend && mvn clean compile -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 6: Commit**

```bash
git add run-planner-backend/
git commit -m "feat: bootstrap Spring Boot backend project"
```

---

## Task 2: Database migrations

**Files:**
- Create: `run-planner-backend/src/main/resources/db/migration/V1__create_users.sql`
- Create: `run-planner-backend/src/main/resources/db/migration/V2__create_refresh_tokens.sql`
- Create: `run-planner-backend/src/main/resources/db/migration/V3__create_goal_races.sql`

Prerequisites: A local PostgreSQL instance running with a `runplanner` database and `runplanner` user. Create them with:
```sql
CREATE USER runplanner WITH PASSWORD 'runplanner';
CREATE DATABASE runplanner OWNER runplanner;
```

- [ ] **Step 1: Create `V1__create_users.sql`**

```sql
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(255),
    date_of_birth   DATE,
    max_hr          INTEGER,
    preferred_units VARCHAR(10)  NOT NULL DEFAULT 'METRIC',
    last_synced_at  TIMESTAMPTZ
);
```

- [ ] **Step 2: Create `V2__create_refresh_tokens.sql`**

```sql
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

- [ ] **Step 3: Create `V3__create_goal_races.sql`**

```sql
CREATE TABLE goal_races (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    distance_meters      INTEGER     NOT NULL,
    distance_label       VARCHAR(50) NOT NULL,
    race_date            DATE        NOT NULL,
    goal_finish_seconds  INTEGER,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goal_races_user_id ON goal_races(user_id);
```

- [ ] **Step 4: Verify migrations run**

```bash
cd run-planner-backend && mvn spring-boot:run 2>&1 | grep -E "Flyway|migration|error|Error" | head -20
```

Press `Ctrl+C` after a few seconds. Look for: `Successfully applied 3 migrations to schema "public"`. If you see `FlywayException` or SQL errors, fix the migration file before continuing.

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/main/resources/db/
git commit -m "feat: add Flyway migrations for users, refresh_tokens, goal_races"
```

---

## Task 3: User entity and repository

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/user/Units.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/user/User.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/user/UserRepository.java`

- [ ] **Step 1: Create `Units.java`**

```java
package com.runplanner.user;

public enum Units {
    METRIC, IMPERIAL
}
```

- [ ] **Step 2: Create `User.java`**

```java
package com.runplanner.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;

    private LocalDate dateOfBirth;

    private Integer maxHr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Units preferredUnits = Units.METRIC;

    private Instant lastSyncedAt;

    // UserDetails implementation — credentials are the passwordHash field
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

- [ ] **Step 3: Create `UserRepository.java`**

```java
package com.runplanner.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd run-planner-backend && mvn clean compile -q
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/user/
git commit -m "feat: add User entity, Units enum, and UserRepository"
```

---

## Task 4: JWT service (TDD)

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/auth/JwtServiceTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/JwtService.java`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd run-planner-backend && mvn test -Dtest=JwtServiceTest -q 2>&1 | tail -5
```

Expected: `BUILD FAILURE` — `JwtService` does not exist yet.

- [ ] **Step 3: Create `JwtService.java`**

```java
package com.runplanner.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry-seconds:3600}")
    private long accessTokenExpirySeconds;

    public String generateAccessToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpirySeconds * 1_000))
            .signWith(signingKey())
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
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

- [ ] **Step 4: Run to confirm tests pass**

```bash
cd run-planner-backend && mvn test -Dtest=JwtServiceTest -q
```

Expected: `BUILD SUCCESS`, 5 tests passing.

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/
git commit -m "feat: add JwtService with access token generation and validation"
```

---

## Task 5: Refresh token entity and auth service (TDD)

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/RefreshToken.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/RefreshTokenRepository.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/dto/RegisterRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/dto/LoginRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/dto/RefreshRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/dto/AuthResponse.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/auth/AuthServiceTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/AuthService.java`

- [ ] **Step 1: Create `RefreshToken.java`**

```java
package com.runplanner.auth;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 2: Create `RefreshTokenRepository.java`**

```java
package com.runplanner.auth;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllByUser(User user);
}
```

- [ ] **Step 3: Create DTOs**

```java
// RegisterRequest.java
package com.runplanner.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    String name
) {}
```

```java
// LoginRequest.java
package com.runplanner.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {}
```

```java
// RefreshRequest.java
package com.runplanner.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

```java
// AuthResponse.java
package com.runplanner.auth.dto;

public record AuthResponse(String accessToken, String refreshToken) {}
```

- [ ] **Step 4: Write the failing tests**

```java
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
```

- [ ] **Step 5: Run to confirm failure**

```bash
cd run-planner-backend && mvn test -Dtest=AuthServiceTest -q 2>&1 | tail -5
```

Expected: `BUILD FAILURE` — `AuthService` does not exist yet.

- [ ] **Step 6: Create `AuthService.java`**

```java
package com.runplanner.auth;

import com.runplanner.auth.dto.AuthResponse;
import com.runplanner.auth.dto.LoginRequest;
import com.runplanner.auth.dto.RegisterRequest;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiry-days:30}")
    private long refreshTokenExpiryDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        var user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .name(request.name())
            .build();
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawToken, String tokenHash) {
        var stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        // Rotate: revoke old token, issue new pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(stored.getUser());
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String rawRefreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
            .user(user)
            .tokenHash(rawRefreshToken) // In production, hash this. See note below.
            .expiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS))
            .build());
        return new AuthResponse(accessToken, rawRefreshToken);
    }
}
```

> **Note on refresh token storage:** For this personal app, the raw token UUID is stored directly. For a public app, store `SHA-256(rawToken)` and compare hashes. The `refresh` method signature accepts both to make future migration straightforward.

- [ ] **Step 7: Run tests to confirm they pass**

```bash
cd run-planner-backend && mvn test -Dtest=AuthServiceTest -q
```

Expected: `BUILD SUCCESS`, 8 tests passing.

- [ ] **Step 8: Commit**

```bash
git add run-planner-backend/src/
git commit -m "feat: add RefreshToken entity, auth DTOs, and AuthService"
```

---

## Task 6: Security config and JWT filter

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/config/JwtAuthFilter.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/config/SecurityConfig.java`

No tests for this task — security config is validated by the controller integration tests in Tasks 7, 8, and 10.

- [ ] **Step 1: Create `JwtAuthFilter.java`**

```java
package com.runplanner.config;

import com.runplanner.auth.JwtService;
import com.runplanner.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        var token = authHeader.substring(7);
        if (jwtService.isValid(token)) {
            var userId = jwtService.extractUserId(token);
            userRepository.findById(userId).ifPresent(user -> {
                var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Create `SecurityConfig.java`**

```java
package com.runplanner.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd run-planner-backend && mvn clean compile -q
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/config/
git commit -m "feat: add JWT filter and Spring Security config"
```

---

## Task 7: Auth controller

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/auth/AuthController.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/auth/AuthControllerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.runplanner.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.auth.dto.AuthResponse;
import com.runplanner.auth.dto.LoginRequest;
import com.runplanner.auth.dto.RefreshRequest;
import com.runplanner.auth.dto.RegisterRequest;
import com.runplanner.config.JwtAuthFilter;
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
@Import(com.runplanner.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtAuthFilter jwtAuthFilter;  // prevents real JWT filter from running in WebMvcTest
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean JwtService jwtService;

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
        when(authService.refresh(any(), any()))
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
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd run-planner-backend && mvn test -Dtest=AuthControllerTest -q 2>&1 | tail -5
```

Expected: `BUILD FAILURE` — `AuthController` does not exist yet.

- [ ] **Step 3: Create `AuthController.java`**

```java
package com.runplanner.auth;

import com.runplanner.auth.dto.AuthResponse;
import com.runplanner.auth.dto.LoginRequest;
import com.runplanner.auth.dto.RefreshRequest;
import com.runplanner.auth.dto.RegisterRequest;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        // Pass the raw token as both args — service handles hashing if needed
        return authService.refresh(request.refreshToken(), request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal User user) {
        authService.logout(user);
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd run-planner-backend && mvn test -Dtest=AuthControllerTest -q
```

Expected: `BUILD SUCCESS`, 6 tests passing.

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/
git commit -m "feat: add AuthController with register, login, refresh, logout"
```

---

## Task 8: User service and controller

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/user/dto/UserResponse.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/user/dto/UpdateProfileRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/user/UserService.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/user/UserController.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/user/UserControllerTest.java`

- [ ] **Step 1: Create DTOs**

```java
// UserResponse.java
package com.runplanner.user.dto;

import com.runplanner.user.Units;
import com.runplanner.user.User;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String name,
    LocalDate dateOfBirth,
    Integer maxHr,
    Units preferredUnits
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(),
            user.getDateOfBirth(), user.getMaxHr(), user.getPreferredUnits());
    }
}
```

```java
// UpdateProfileRequest.java
package com.runplanner.user.dto;

import com.runplanner.user.Units;

import java.time.LocalDate;

public record UpdateProfileRequest(
    String name,
    LocalDate dateOfBirth,
    Integer maxHr,
    Units preferredUnits
) {}
```

- [ ] **Step 2: Create `UserService.java`**

```java
package com.runplanner.user;

import com.runplanner.user.dto.UpdateProfileRequest;
import com.runplanner.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile(User user) {
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.name() != null) user.setName(request.name());
        if (request.dateOfBirth() != null) user.setDateOfBirth(request.dateOfBirth());
        if (request.maxHr() != null) user.setMaxHr(request.maxHr());
        if (request.preferredUnits() != null) user.setPreferredUnits(request.preferredUnits());
        return UserResponse.from(userRepository.save(user));
    }
}
```

- [ ] **Step 3: Write the failing controller tests**

```java
package com.runplanner.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.config.JwtAuthFilter;
import com.runplanner.user.dto.UpdateProfileRequest;
import com.runplanner.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(com.runplanner.config.SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User authenticatedUser() {
        return User.builder().id(UUID.randomUUID()).email("u@x.com").passwordHash("h").build();
    }

    @Test
    void getProfile_returns200WithUserData() throws Exception {
        var user = authenticatedUser();
        var response = new UserResponse(user.getId(), user.getEmail(), "Alice", null, 185, Units.METRIC);
        when(userService.getProfile(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                .with(SecurityMockMvcRequestPostProcessors.user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("u@x.com"))
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getProfile_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_returns200WithUpdatedData() throws Exception {
        var user = authenticatedUser();
        var updated = new UserResponse(user.getId(), user.getEmail(), "Bob", null, 190, Units.IMPERIAL);
        when(userService.updateProfile(any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/users/me")
                .with(SecurityMockMvcRequestPostProcessors.user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateProfileRequest("Bob", null, 190, Units.IMPERIAL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bob"))
            .andExpect(jsonPath("$.maxHr").value(190));
    }
}
```

- [ ] **Step 4: Run to confirm failure**

```bash
cd run-planner-backend && mvn test -Dtest=UserControllerTest -q 2>&1 | tail -5
```

Expected: `BUILD FAILURE` — `UserController` does not exist yet.

- [ ] **Step 5: Create `UserController.java`**

```java
package com.runplanner.user;

import com.runplanner.user.dto.UpdateProfileRequest;
import com.runplanner.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getProfile(@AuthenticationPrincipal User user) {
        return userService.getProfile(user);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal User user,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user, request);
    }
}
```

- [ ] **Step 6: Run tests to confirm they pass**

```bash
cd run-planner-backend && mvn test -Dtest=UserControllerTest -q
```

Expected: `BUILD SUCCESS`, 3 tests passing.

- [ ] **Step 7: Commit**

```bash
git add run-planner-backend/src/
git commit -m "feat: add UserService and UserController for profile management"
```

---

## Task 9: Goal race service (TDD)

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/GoalRaceStatus.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/GoalRace.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/GoalRaceRepository.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/dto/CreateGoalRaceRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/dto/UpdateGoalRaceRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/dto/GoalRaceResponse.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/goalrace/GoalRaceServiceTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/GoalRaceService.java`

- [ ] **Step 1: Create `GoalRaceStatus.java`**

```java
package com.runplanner.goalrace;

public enum GoalRaceStatus {
    ACTIVE, COMPLETED, ARCHIVED
}
```

- [ ] **Step 2: Create `GoalRace.java`**

```java
package com.runplanner.goalrace;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goal_races")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer distanceMeters;

    @Column(nullable = false)
    private String distanceLabel;

    @Column(nullable = false)
    private LocalDate raceDate;

    private Integer goalFinishSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoalRaceStatus status = GoalRaceStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 3: Create `GoalRaceRepository.java`**

```java
package com.runplanner.goalrace;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRaceRepository extends JpaRepository<GoalRace, UUID> {
    List<GoalRace> findAllByUserOrderByRaceDateDesc(User user);
    Optional<GoalRace> findByIdAndUser(UUID id, User user);
}
```

- [ ] **Step 4: Create DTOs**

```java
// CreateGoalRaceRequest.java
package com.runplanner.goalrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateGoalRaceRequest(
    @NotNull @Positive Integer distanceMeters,
    @NotBlank String distanceLabel,
    @NotNull LocalDate raceDate,
    Integer goalFinishSeconds
) {}
```

```java
// UpdateGoalRaceRequest.java
package com.runplanner.goalrace.dto;

import com.runplanner.goalrace.GoalRaceStatus;

import java.time.LocalDate;

public record UpdateGoalRaceRequest(
    LocalDate raceDate,
    Integer goalFinishSeconds,
    GoalRaceStatus status
) {}
```

```java
// GoalRaceResponse.java
package com.runplanner.goalrace.dto;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record GoalRaceResponse(
    UUID id,
    Integer distanceMeters,
    String distanceLabel,
    LocalDate raceDate,
    Integer goalFinishSeconds,
    GoalRaceStatus status
) {
    public static GoalRaceResponse from(GoalRace race) {
        return new GoalRaceResponse(race.getId(), race.getDistanceMeters(), race.getDistanceLabel(),
            race.getRaceDate(), race.getGoalFinishSeconds(), race.getStatus());
    }
}
```

- [ ] **Step 5: Write the failing tests**

```java
package com.runplanner.goalrace;

import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalRaceServiceTest {

    @Mock GoalRaceRepository goalRaceRepository;
    @InjectMocks GoalRaceService goalRaceService;

    private User user() {
        return User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void create_savesAndReturnsGoalRace() {
        var user = user();
        var request = new CreateGoalRaceRequest(21097, "Half Marathon",
            LocalDate.of(2026, 10, 1), 6600);
        when(goalRaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = goalRaceService.create(user, request);

        assertThat(response.distanceMeters()).isEqualTo(21097);
        assertThat(response.distanceLabel()).isEqualTo("Half Marathon");
        assertThat(response.status()).isEqualTo(GoalRaceStatus.ACTIVE);
        verify(goalRaceRepository).save(any());
    }

    @Test
    void findAll_returnsAllRacesForUser() {
        var user = user();
        var race = GoalRace.builder().id(UUID.randomUUID()).user(user)
            .distanceMeters(5000).distanceLabel("5K")
            .raceDate(LocalDate.now()).build();
        when(goalRaceRepository.findAllByUserOrderByRaceDateDesc(user)).thenReturn(List.of(race));

        var responses = goalRaceService.findAll(user);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).distanceLabel()).isEqualTo("5K");
    }

    @Test
    void update_modifiesMutableFields() {
        var user = user();
        var race = GoalRace.builder().id(UUID.randomUUID()).user(user)
            .distanceMeters(5000).distanceLabel("5K")
            .raceDate(LocalDate.of(2026, 6, 1))
            .status(GoalRaceStatus.ACTIVE).build();
        when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
        when(goalRaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = goalRaceService.update(user, race.getId(),
            new UpdateGoalRaceRequest(LocalDate.of(2026, 9, 1), 1200, GoalRaceStatus.ARCHIVED));

        assertThat(updated.raceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(updated.status()).isEqualTo(GoalRaceStatus.ARCHIVED);
    }

    @Test
    void update_throws404WhenNotFound() {
        var user = user();
        when(goalRaceRepository.findByIdAndUser(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> goalRaceService.update(user, UUID.randomUUID(),
            new UpdateGoalRaceRequest(null, null, null)))
            .isInstanceOf(ResponseStatusException.class);
    }
}
```

- [ ] **Step 6: Run to confirm failure**

```bash
cd run-planner-backend && mvn test -Dtest=GoalRaceServiceTest -q 2>&1 | tail -5
```

Expected: `BUILD FAILURE` — `GoalRaceService` does not exist yet.

- [ ] **Step 7: Create `GoalRaceService.java`**

```java
package com.runplanner.goalrace;

import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.GoalRaceResponse;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalRaceService {

    private final GoalRaceRepository goalRaceRepository;

    @Transactional
    public GoalRaceResponse create(User user, CreateGoalRaceRequest request) {
        var race = GoalRace.builder()
            .user(user)
            .distanceMeters(request.distanceMeters())
            .distanceLabel(request.distanceLabel())
            .raceDate(request.raceDate())
            .goalFinishSeconds(request.goalFinishSeconds())
            .build();
        return GoalRaceResponse.from(goalRaceRepository.save(race));
    }

    public List<GoalRaceResponse> findAll(User user) {
        return goalRaceRepository.findAllByUserOrderByRaceDateDesc(user)
            .stream()
            .map(GoalRaceResponse::from)
            .toList();
    }

    @Transactional
    public GoalRaceResponse update(User user, UUID id, UpdateGoalRaceRequest request) {
        var race = goalRaceRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal race not found"));
        if (request.raceDate() != null) race.setRaceDate(request.raceDate());
        if (request.goalFinishSeconds() != null) race.setGoalFinishSeconds(request.goalFinishSeconds());
        if (request.status() != null) race.setStatus(request.status());
        return GoalRaceResponse.from(goalRaceRepository.save(race));
    }
}
```

- [ ] **Step 8: Run tests to confirm they pass**

```bash
cd run-planner-backend && mvn test -Dtest=GoalRaceServiceTest -q
```

Expected: `BUILD SUCCESS`, 4 tests passing.

- [ ] **Step 9: Commit**

```bash
git add run-planner-backend/src/
git commit -m "feat: add GoalRace entity, repository, DTOs, and GoalRaceService"
```

---

## Task 10: Goal race controller

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/goalrace/GoalRaceController.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/goalrace/GoalRaceControllerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.runplanner.goalrace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.config.JwtAuthFilter;
import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.GoalRaceResponse;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalRaceController.class)
@Import(com.runplanner.config.SecurityConfig.class)
class GoalRaceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean GoalRaceService goalRaceService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("u@x.com").passwordHash("h").build();
    }

    @Test
    void createGoalRace_returns201() throws Exception {
        var user = user();
        var response = new GoalRaceResponse(UUID.randomUUID(), 21097, "Half Marathon",
            LocalDate.of(2026, 10, 1), 6600, GoalRaceStatus.ACTIVE);
        when(goalRaceService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/goal-races")
                .with(SecurityMockMvcRequestPostProcessors.user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateGoalRaceRequest(21097, "Half Marathon",
                        LocalDate.of(2026, 10, 1), 6600))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.distanceLabel").value("Half Marathon"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createGoalRace_returns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/goal-races")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listGoalRaces_returnsAll() throws Exception {
        var user = user();
        var race = new GoalRaceResponse(UUID.randomUUID(), 5000, "5K",
            LocalDate.of(2026, 6, 1), null, GoalRaceStatus.ACTIVE);
        when(goalRaceService.findAll(any())).thenReturn(List.of(race));

        mockMvc.perform(get("/api/v1/goal-races")
                .with(SecurityMockMvcRequestPostProcessors.user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].distanceLabel").value("5K"));
    }

    @Test
    void updateGoalRace_returns200() throws Exception {
        var user = user();
        var raceId = UUID.randomUUID();
        var updated = new GoalRaceResponse(raceId, 5000, "5K",
            LocalDate.of(2026, 9, 1), null, GoalRaceStatus.ARCHIVED);
        when(goalRaceService.update(any(), any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/goal-races/" + raceId)
                .with(SecurityMockMvcRequestPostProcessors.user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateGoalRaceRequest(LocalDate.of(2026, 9, 1), null, GoalRaceStatus.ARCHIVED))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd run-planner-backend && mvn test -Dtest=GoalRaceControllerTest -q 2>&1 | tail -5
```

Expected: `BUILD FAILURE` — `GoalRaceController` does not exist yet.

- [ ] **Step 3: Create `GoalRaceController.java`**

```java
package com.runplanner.goalrace;

import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.GoalRaceResponse;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goal-races")
@RequiredArgsConstructor
public class GoalRaceController {

    private final GoalRaceService goalRaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalRaceResponse create(@AuthenticationPrincipal User user,
                                   @Valid @RequestBody CreateGoalRaceRequest request) {
        return goalRaceService.create(user, request);
    }

    @GetMapping
    public List<GoalRaceResponse> list(@AuthenticationPrincipal User user) {
        return goalRaceService.findAll(user);
    }

    @PatchMapping("/{id}")
    public GoalRaceResponse update(@AuthenticationPrincipal User user,
                                   @PathVariable UUID id,
                                   @RequestBody UpdateGoalRaceRequest request) {
        return goalRaceService.update(user, id, request);
    }
}
```

- [ ] **Step 4: Run all tests**

```bash
cd run-planner-backend && mvn test -q
```

Expected: `BUILD SUCCESS`. All tests across all packages pass.

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/
git commit -m "feat: add GoalRaceController — backend foundation complete"
```

---

## Done

The backend foundation is complete. Verify the full test suite one final time:

```bash
cd run-planner-backend && mvn test
```

Expected output includes:
- `JwtServiceTest` — 5 tests
- `AuthServiceTest` — 8 tests
- `AuthControllerTest` — 6 tests
- `UserControllerTest` — 3 tests
- `GoalRaceServiceTest` — 4 tests
- `GoalRaceControllerTest` — 4 tests

**Next plan:** `2026-03-24-vdot-engine.md` — VDOT math engine, training zones, pace calculations, and VDOT history tracking.
