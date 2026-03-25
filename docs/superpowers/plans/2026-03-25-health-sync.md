# Health Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the health sync endpoint that ingests workouts and health snapshots from the Flutter client, orchestrating deduplication, matching, VDOT recalculation, and VO2max-based VDOT seeding.

**Architecture:** `HealthSnapshot` entity in a `health` package with a `HealthSyncService` orchestrator that delegates to existing services (`WorkoutService`, `WorkoutMatcher`, `VdotHistoryService`). A single `POST /api/v1/health/sync` endpoint accepts both workouts and snapshots, returns a sync summary. Flyway V9 creates the `health_snapshots` table and adds the FK from `vdot_history`.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL, Flyway, JUnit 5, Mockito, AssertJ, MockMvc, Lombok

**Spec:** `docs/superpowers/specs/2026-03-25-health-sync-design.md`

---

### Task 1: Database Migration (V9)

**Files:**
- Create: `src/main/resources/db/migration/V9__create_health_snapshots.sql`

- [ ] **Step 1: Create V9 migration**

```sql
CREATE TABLE health_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vo2max_estimate DOUBLE PRECISION,
    resting_hr      INTEGER,
    recorded_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_health_snapshots_user_id ON health_snapshots(user_id);
CREATE INDEX idx_health_snapshots_user_recorded_at ON health_snapshots(user_id, recorded_at);

ALTER TABLE vdot_history
    ADD CONSTRAINT fk_vdot_triggering_snapshot
    FOREIGN KEY (triggering_snapshot_id) REFERENCES health_snapshots(id);
```

- [ ] **Step 2: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V9__create_health_snapshots.sql
git commit -m "feat(health): add V9 migration for health_snapshots table"
```

---

### Task 2: HealthSnapshot Entity and Repository

**Files:**
- Create: `src/main/java/com/runplanner/health/HealthSnapshot.java`
- Create: `src/main/java/com/runplanner/health/HealthSnapshotRepository.java`

- [ ] **Step 1: Create HealthSnapshot entity**

```java
package com.runplanner.health;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double vo2maxEstimate;

    private Integer restingHr;

    @Column(nullable = false)
    private Instant recordedAt;
}
```

- [ ] **Step 2: Create HealthSnapshotRepository**

```java
package com.runplanner.health;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthSnapshotRepository extends JpaRepository<HealthSnapshot, UUID> {

    Optional<HealthSnapshot> findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(User user);

    List<HealthSnapshot> findAllByUserOrderByRecordedAtDesc(User user);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/runplanner/health/
git commit -m "feat(health): add HealthSnapshot entity and repository"
```

---

### Task 3: DTOs

**Files:**
- Create: `src/main/java/com/runplanner/health/dto/HealthSyncRequest.java`
- Create: `src/main/java/com/runplanner/health/dto/HealthSyncResponse.java`

- [ ] **Step 1: Create HealthSyncRequest**

```java
package com.runplanner.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record HealthSyncRequest(
        @Valid List<WorkoutSyncItem> workouts,
        @Valid List<HealthSnapshotSyncItem> healthSnapshots
) {
    public record WorkoutSyncItem(
            @NotBlank String source,
            String sourceId,
            @NotNull Instant startedAt,
            @NotNull Double distanceMeters,
            @NotNull Integer durationSeconds,
            Integer avgHr,
            Integer maxHr,
            Double elevationGain
    ) {}

    public record HealthSnapshotSyncItem(
            Double vo2maxEstimate,
            Integer restingHr,
            @NotNull Instant recordedAt
    ) {}
}
```

- [ ] **Step 2: Create HealthSyncResponse**

```java
package com.runplanner.health.dto;

public record HealthSyncResponse(
        int workoutsSaved,
        int workoutsSkipped,
        int workoutsMatched,
        int snapshotsSaved,
        boolean vdotUpdated
) {}
```

- [ ] **Step 3: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/runplanner/health/dto/
git commit -m "feat(health): add HealthSyncRequest and HealthSyncResponse DTOs"
```

---

### Task 4: HealthSyncService — Tests

**Files:**
- Create: `src/test/java/com/runplanner/health/HealthSyncServiceTest.java`

- [ ] **Step 1: Write test class with helpers and all tests**

```java
package com.runplanner.health;

import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncRequest.HealthSnapshotSyncItem;
import com.runplanner.health.dto.HealthSyncRequest.WorkoutSyncItem;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.match.WorkoutMatch;
import com.runplanner.match.WorkoutMatcher;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import com.runplanner.vdot.VdotHistory;
import com.runplanner.vdot.VdotHistoryService;
import com.runplanner.workout.Workout;
import com.runplanner.workout.WorkoutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthSyncServiceTest {

    @Mock private WorkoutService workoutService;
    @Mock private WorkoutMatcher workoutMatcher;
    @Mock private HealthSnapshotRepository healthSnapshotRepository;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private UserRepository userRepository;

    @InjectMocks private HealthSyncService service;

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("h")
                .build();
    }

    private WorkoutSyncItem workoutItem(String sourceId) {
        return new WorkoutSyncItem(
                "APPLE_HEALTH", sourceId, Instant.parse("2026-03-25T08:00:00Z"),
                10000.0, 3000, 155, 172, 45.0);
    }

    private HealthSnapshotSyncItem snapshotItem(Double vo2max) {
        return new HealthSnapshotSyncItem(vo2max, 52, Instant.parse("2026-03-25T06:00:00Z"));
    }

    // --- Workout ingestion ---

    @Test
    void sync_workoutsOnly_savesNonDuplicatesAndSkipsDuplicates() {
        User user = user();
        when(workoutService.existsBySourceAndSourceId("APPLE_HEALTH", "new-1")).thenReturn(false);
        when(workoutService.existsBySourceAndSourceId("APPLE_HEALTH", "dup-1")).thenReturn(true);
        when(workoutService.save(any())).thenAnswer(inv -> {
            Workout w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(workoutMatcher.match(any())).thenReturn(Optional.empty());

        HealthSyncRequest request = new HealthSyncRequest(
                List.of(workoutItem("new-1"), workoutItem("dup-1")), null);

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isEqualTo(1);
        assertThat(response.workoutsSkipped()).isEqualTo(1);
        verify(workoutService, times(1)).save(any());
    }

    @Test
    void sync_matching_newWorkoutsPassedToMatcher() {
        User user = user();
        when(workoutService.existsBySourceAndSourceId(any(), any())).thenReturn(false);
        when(workoutService.save(any())).thenAnswer(inv -> {
            Workout w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(workoutMatcher.match(any())).thenReturn(
                Optional.of(WorkoutMatch.builder().complianceScore(0.9).build()));

        HealthSyncRequest request = new HealthSyncRequest(
                List.of(workoutItem("w-1")), null);

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsMatched()).isEqualTo(1);
        verify(workoutMatcher).match(any());
    }

    // --- Snapshot ingestion ---

    @Test
    void sync_snapshotsOnly_savesAll() {
        User user = user();
        when(healthSnapshotRepository.save(any())).thenAnswer(inv -> {
            HealthSnapshot s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.empty());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5), snapshotItem(null)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.snapshotsSaved()).isEqualTo(2);
        verify(healthSnapshotRepository, times(2)).save(any());
    }

    // --- VDOT from VO2max ---

    @Test
    void sync_vo2maxSnapshot_triggersVdotRecording() {
        User user = user();
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(48.5)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(45.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(45.0), eq(48.5), isNull(), eq(snapshot.getId())))
                .thenReturn(VdotHistory.builder().newVdot(48.5).build());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isTrue();
        verify(vdotHistoryService).recordCalculation(user, 45.0, 48.5, null, snapshot.getId());
    }

    @Test
    void sync_vo2maxEqualsCurrentVdot_skipsRecording() {
        User user = user();
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(48.5)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.5));

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isFalse();
        verify(vdotHistoryService, never()).recordCalculation(any(), anyDouble(), anyDouble(), any(), any());
    }

    @Test
    void sync_noVo2maxInSnapshots_skipsVdotRecording() {
        User user = user();
        when(healthSnapshotRepository.save(any())).thenAnswer(inv -> {
            HealthSnapshot s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.empty());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(null)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isFalse();
        verify(vdotHistoryService, never()).recordCalculation(any(), anyDouble(), anyDouble(), any(), any());
    }

    @Test
    void sync_vo2maxWithNoExistingVdot_recordsWithZeroPrevious() {
        User user = user();
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(48.5)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());
        when(vdotHistoryService.recordCalculation(eq(user), eq(0.0), eq(48.5), isNull(), eq(snapshot.getId())))
                .thenReturn(VdotHistory.builder().newVdot(48.5).build());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isTrue();
        verify(vdotHistoryService).recordCalculation(user, 0.0, 48.5, null, snapshot.getId());
    }

    // --- lastSyncedAt ---

    @Test
    void sync_updatesLastSyncedAt() {
        User user = user();

        HealthSyncRequest request = new HealthSyncRequest(null, null);
        service.sync(user, request);

        assertThat(user.getLastSyncedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    // --- Empty/null handling ---

    @Test
    void sync_emptyRequest_returnsZerosAndUpdatesLastSynced() {
        User user = user();

        HealthSyncRequest request = new HealthSyncRequest(List.of(), List.of());
        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isZero();
        assertThat(response.workoutsSkipped()).isZero();
        assertThat(response.workoutsMatched()).isZero();
        assertThat(response.snapshotsSaved()).isZero();
        assertThat(response.vdotUpdated()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void sync_nullLists_treatedAsEmpty() {
        User user = user();

        HealthSyncRequest request = new HealthSyncRequest(null, null);
        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isZero();
        assertThat(response.snapshotsSaved()).isZero();
        verify(userRepository).save(user);
    }

    // --- Full flow ---

    @Test
    void sync_bothWorkoutsAndSnapshots_fullFlow() {
        User user = user();
        when(workoutService.existsBySourceAndSourceId(any(), any())).thenReturn(false);
        when(workoutService.save(any())).thenAnswer(inv -> {
            Workout w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(workoutMatcher.match(any())).thenReturn(Optional.empty());
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(50.0)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(any(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        HealthSyncRequest request = new HealthSyncRequest(
                List.of(workoutItem("w-1")),
                List.of(snapshotItem(50.0)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isEqualTo(1);
        assertThat(response.snapshotsSaved()).isEqualTo(1);
        assertThat(response.vdotUpdated()).isTrue();
        verify(userRepository).save(user);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=HealthSyncServiceTest test`
Expected: COMPILATION FAILURE (HealthSyncService does not exist yet)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/runplanner/health/HealthSyncServiceTest.java
git commit -m "test(health): add HealthSyncService unit tests"
```

---

### Task 5: HealthSyncService — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/health/HealthSyncService.java`

- [ ] **Step 1: Implement HealthSyncService**

```java
package com.runplanner.health;

import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncRequest.HealthSnapshotSyncItem;
import com.runplanner.health.dto.HealthSyncRequest.WorkoutSyncItem;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.match.WorkoutMatcher;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import com.runplanner.vdot.VdotHistoryService;
import com.runplanner.workout.Workout;
import com.runplanner.workout.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthSyncService {

    private final WorkoutService workoutService;
    private final WorkoutMatcher workoutMatcher;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final VdotHistoryService vdotHistoryService;
    private final UserRepository userRepository;

    @Transactional
    public HealthSyncResponse sync(User user, HealthSyncRequest request) {
        int workoutsSaved = 0;
        int workoutsSkipped = 0;
        int workoutsMatched = 0;
        int snapshotsSaved = 0;
        boolean vdotUpdated = false;

        // 1. Ingest workouts
        List<Workout> newWorkouts = new ArrayList<>();
        if (request.workouts() != null) {
            for (WorkoutSyncItem item : request.workouts()) {
                if (item.sourceId() != null
                        && workoutService.existsBySourceAndSourceId(item.source(), item.sourceId())) {
                    workoutsSkipped++;
                    continue;
                }

                Workout workout = Workout.builder()
                        .user(user)
                        .source(item.source())
                        .sourceId(item.sourceId())
                        .startedAt(item.startedAt())
                        .distanceMeters(item.distanceMeters())
                        .durationSeconds(item.durationSeconds())
                        .avgHr(item.avgHr())
                        .maxHr(item.maxHr())
                        .elevationGain(item.elevationGain())
                        .build();

                Workout saved = workoutService.save(workout);
                newWorkouts.add(saved);
                workoutsSaved++;
            }
        }

        // 2. Match new workouts
        for (Workout workout : newWorkouts) {
            if (workoutMatcher.match(workout).isPresent()) {
                workoutsMatched++;
            }
        }

        // 3. Ingest health snapshots
        if (request.healthSnapshots() != null) {
            for (HealthSnapshotSyncItem item : request.healthSnapshots()) {
                HealthSnapshot snapshot = HealthSnapshot.builder()
                        .user(user)
                        .vo2maxEstimate(item.vo2maxEstimate())
                        .restingHr(item.restingHr())
                        .recordedAt(item.recordedAt())
                        .build();
                healthSnapshotRepository.save(snapshot);
                snapshotsSaved++;
            }
        }

        // 4. VDOT from VO2max
        var latestVo2max = healthSnapshotRepository
                .findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user);
        if (latestVo2max.isPresent()) {
            double newVdot = latestVo2max.get().getVo2maxEstimate();
            double previousVdot = vdotHistoryService.getEffectiveVdot(user).orElse(0.0);

            if (newVdot != previousVdot) {
                vdotHistoryService.recordCalculation(
                        user, previousVdot, newVdot, null, latestVo2max.get().getId());
                vdotUpdated = true;
            }
        }

        // 5. Update lastSyncedAt
        user.setLastSyncedAt(Instant.now());
        userRepository.save(user);

        // 6. Return summary
        return new HealthSyncResponse(
                workoutsSaved, workoutsSkipped, workoutsMatched, snapshotsSaved, vdotUpdated);
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=HealthSyncServiceTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/health/HealthSyncService.java
git commit -m "feat(health): implement HealthSyncService orchestrating sync pipeline"
```

---

### Task 6: HealthSyncController — Tests

**Files:**
- Create: `src/test/java/com/runplanner/health/HealthSyncControllerTest.java`

- [ ] **Step 1: Write controller tests**

```java
package com.runplanner.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.config.SecurityConfig;
import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthSyncController.class)
@Import(SecurityConfig.class)
class HealthSyncControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean HealthSyncService healthSyncService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").build();
    }

    @Test
    void sync_returns200WithSummary() throws Exception {
        User u = testUser();
        when(healthSyncService.sync(any(), any()))
                .thenReturn(new HealthSyncResponse(2, 1, 1, 1, true));

        mockMvc.perform(post("/api/v1/health/sync")
                        .with(user(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HealthSyncRequest(List.of(), List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutsSaved").value(2))
                .andExpect(jsonPath("$.workoutsSkipped").value(1))
                .andExpect(jsonPath("$.workoutsMatched").value(1))
                .andExpect(jsonPath("$.snapshotsSaved").value(1))
                .andExpect(jsonPath("$.vdotUpdated").value(true));
    }

    @Test
    void sync_emptyBody_returns200() throws Exception {
        User u = testUser();
        when(healthSyncService.sync(any(), any()))
                .thenReturn(new HealthSyncResponse(0, 0, 0, 0, false));

        mockMvc.perform(post("/api/v1/health/sync")
                        .with(user(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutsSaved").value(0));
    }

    @Test
    void sync_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/health/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=HealthSyncControllerTest test`
Expected: COMPILATION FAILURE (HealthSyncController does not exist yet)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/runplanner/health/HealthSyncControllerTest.java
git commit -m "test(health): add HealthSyncController integration tests"
```

---

### Task 7: HealthSyncController — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/health/HealthSyncController.java`

- [ ] **Step 1: Implement HealthSyncController**

```java
package com.runplanner.health;

import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthSyncController {

    private final HealthSyncService healthSyncService;

    @PostMapping("/sync")
    public HealthSyncResponse sync(@AuthenticationPrincipal User user,
                                    @Valid @RequestBody HealthSyncRequest request) {
        return healthSyncService.sync(user, request);
    }
}
```

- [ ] **Step 2: Run controller tests**

Run: `cd run-planner-backend && mvn -Dtest=HealthSyncControllerTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/health/HealthSyncController.java
git commit -m "feat(health): add HealthSyncController with POST /health/sync endpoint"
```

---

### Task 8: Full Test Suite Verification

- [ ] **Step 1: Run all tests**

Run: `cd run-planner-backend && mvn test`
Expected: All tests PASS (including all existing tests and new health sync tests)

- [ ] **Step 2: Verify build**

Run: `cd run-planner-backend && mvn clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 3: Final commit if any issues found**

If the full build reveals any issues, fix and commit:

```bash
git add -A
git commit -m "chore: fix issues from full build verification"
```
