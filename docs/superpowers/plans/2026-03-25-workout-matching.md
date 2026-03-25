# Workout Matching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement workout matching that pairs completed workouts to planned workouts and calculates a compliance score based on distance, pace, and HR accuracy.

**Architecture:** `WorkoutMatch` entity in a `match` package bridges workouts and planned workouts. `ComplianceScorer` is a pure math class calculating the 3-factor weighted score. `WorkoutMatcher` is the service that finds candidates within ±1 day and picks the best match. No REST endpoints — purely internal for the future health sync and adjustment engine.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL, Flyway, JUnit 5, Mockito, AssertJ, Lombok

**Spec:** `docs/superpowers/specs/2026-03-25-workout-matching-design.md`

---

### Task 1: Database Migration (V8)

**Files:**
- Create: `src/main/resources/db/migration/V8__create_workout_matches.sql`

- [ ] **Step 1: Create V8 migration**

```sql
CREATE TABLE workout_matches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planned_workout_id  UUID NOT NULL REFERENCES planned_workouts(id) ON DELETE CASCADE,
    workout_id          UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    compliance_score    DOUBLE PRECISION NOT NULL
);

CREATE UNIQUE INDEX idx_workout_matches_planned ON workout_matches(planned_workout_id);
CREATE UNIQUE INDEX idx_workout_matches_workout ON workout_matches(workout_id);
```

- [ ] **Step 2: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V8__create_workout_matches.sql
git commit -m "feat(match): add V8 migration for workout_matches table"
```

---

### Task 2: Entity and Repository

**Files:**
- Create: `src/main/java/com/runplanner/match/WorkoutMatch.java`
- Create: `src/main/java/com/runplanner/match/WorkoutMatchRepository.java`

- [ ] **Step 1: Create WorkoutMatch entity**

```java
package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.workout.Workout;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "workout_matches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_workout_id", nullable = false)
    private PlannedWorkout plannedWorkout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(nullable = false)
    private double complianceScore;
}
```

- [ ] **Step 2: Create WorkoutMatchRepository**

```java
package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.workout.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutMatchRepository extends JpaRepository<WorkoutMatch, UUID> {

    Optional<WorkoutMatch> findByPlannedWorkout(PlannedWorkout plannedWorkout);

    Optional<WorkoutMatch> findByWorkout(Workout workout);

    boolean existsByPlannedWorkout(PlannedWorkout plannedWorkout);

    boolean existsByWorkout(Workout workout);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/runplanner/match/
git commit -m "feat(match): add WorkoutMatch entity and repository"
```

---

### Task 3: MatchConstants

**Files:**
- Create: `src/main/java/com/runplanner/match/MatchConstants.java`

- [ ] **Step 1: Create MatchConstants**

```java
package com.runplanner.match;

public final class MatchConstants {

    private MatchConstants() {}

    // Compliance score factor weights (must sum to 1.0)
    public static final double DISTANCE_WEIGHT = 0.4;
    public static final double PACE_WEIGHT = 0.4;
    public static final double HR_WEIGHT = 0.2;

    // HR zone boundaries as percentages of max HR
    // Zone 1 (Easy): 65-79%
    // Zone 2 (Marathon): 80-85%
    // Zone 3 (Threshold): 86-90%
    // Zone 4 (Interval): 91-95%
    // Zone 5 (Repetition): 96-100%
    public static final double[] HR_ZONE_LOWER_BOUNDS = {0.65, 0.80, 0.86, 0.91, 0.96};
    public static final double[] HR_ZONE_UPPER_BOUNDS = {0.79, 0.85, 0.90, 0.95, 1.00};

    // HR zone scoring
    public static final double HR_ZONE_MATCH = 1.0;
    public static final double HR_ZONE_ADJACENT = 0.5;
    public static final double HR_ZONE_FAR = 0.0;

    // Default factor value when data is missing
    public static final double DEFAULT_FACTOR = 1.0;

    // Matching window: ±1 day
    public static final int MATCH_WINDOW_DAYS = 1;
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/match/MatchConstants.java
git commit -m "feat(match): add MatchConstants for scoring weights and HR zone boundaries"
```

---

### Task 4: ComplianceScorer — Tests

**Files:**
- Create: `src/test/java/com/runplanner/match/ComplianceScorerTest.java`

- [ ] **Step 1: Write test class with helpers**

```java
package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ComplianceScorerTest {

    private final ComplianceScorer scorer = new ComplianceScorer();

    private User user(Integer maxHr) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("h")
                .maxHr(maxHr)
                .build();
    }

    private Workout workout(User user, double distanceMeters, int durationSeconds, Integer avgHr) {
        return Workout.builder()
                .id(UUID.randomUUID())
                .user(user)
                .source("TEST")
                .startedAt(Instant.now())
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .avgHr(avgHr)
                .build();
    }

    private PlannedWorkout planned(WorkoutType type, double distanceMeters,
                                    Double paceMin, Double paceMax, Integer hrZone) {
        return PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .weekNumber(1)
                .dayOfWeek(2)
                .scheduledDate(LocalDate.of(2026, 3, 25))
                .workoutType(type)
                .targetDistanceMeters(distanceMeters)
                .targetPaceMinPerKm(paceMin)
                .targetPaceMaxPerKm(paceMax)
                .targetHrZone(hrZone)
                .originalScheduledDate(LocalDate.of(2026, 3, 25))
                .build();
    }
}
```

- [ ] **Step 2: Write REST and perfect match tests**

Add to test class:

```java
@Test
void score_restPlannedWorkout_returnsZero() {
    User user = user(180);
    Workout w = workout(user, 5000.0, 1500, 140);
    PlannedWorkout pw = planned(WorkoutType.REST, 0, null, null, null);

    double score = scorer.score(w, pw);

    assertThat(score).isEqualTo(0.0);
}

@Test
void score_perfectMatch_returnsOne() {
    User user = user(180);
    // Planned: 10km at 5.0-5.5 min/km, HR zone 1 (easy, 65-79% of 180 = 117-142)
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    // Actual: exactly 10km, pace = 1500s / (10km) = 2.5 min/km...
    // Need pace of 5.25 min/km (midpoint) for 10km: duration = 5.25 * 10 * 60 = 3150s
    Workout w = workout(user, 10000.0, 3150, 130);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.01));
}
```

- [ ] **Step 3: Write distance factor tests**

Add to test class:

```java
@Test
void score_distanceUnderTarget_proportionalFactor() {
    User user = user(180);
    // 80% of target distance, perfect pace and HR
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    // 8000m at 5.25 min/km: duration = 5.25 * 8 * 60 = 2520s
    Workout w = workout(user, 8000.0, 2520, 130);

    double score = scorer.score(w, pw);

    // Distance factor: 0.8 * 0.4 = 0.32, pace factor: ~1.0 * 0.4 = 0.4, HR: 1.0 * 0.2 = 0.2
    assertThat(score).isCloseTo(0.92, within(0.02));
}

@Test
void score_distanceOverTarget_cappedAtOne() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    // 12000m at 5.25 min/km: duration = 5.25 * 12 * 60 = 3780s
    Workout w = workout(user, 12000.0, 3780, 130);

    double score = scorer.score(w, pw);

    // Distance capped at 1.0
    assertThat(score).isCloseTo(1.0, within(0.01));
}
```

- [ ] **Step 4: Write pace factor tests**

Add to test class:

```java
@Test
void score_paceAtMidpoint_fullPaceFactor() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    // Midpoint = 5.25 min/km for 10km: 3150s
    Workout w = workout(user, 10000.0, 3150, 130);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.01));
}

@Test
void score_paceAtEdgeOfRange_zeroPaceFactor() {
    User user = user(180);
    // Range: 5.0-5.5, range width = 0.5, midpoint = 5.25
    // Edge: midpoint + range/2 = 5.25 + 0.25 = 5.5 min/km
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    // 5.5 min/km for 10km: 5.5 * 10 * 60 = 3300s
    Workout w = workout(user, 10000.0, 3300, 130);

    double score = scorer.score(w, pw);

    // Pace deviation = |5.5 - 5.25| / 0.5 = 0.5 → pace factor = 0.5
    // Total: 1.0*0.4 + 0.5*0.4 + 1.0*0.2 = 0.8
    assertThat(score).isCloseTo(0.8, within(0.02));
}

@Test
void score_paceOutsideRange_zeroPaceFactor() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    // 7.0 min/km for 10km: 7.0 * 10 * 60 = 4200s (way too slow)
    Workout w = workout(user, 10000.0, 4200, 130);

    double score = scorer.score(w, pw);

    // Pace deviation = |7.0 - 5.25| / 0.5 = 3.5, clamped to 1.0 → pace factor = 0.0
    // Total: 1.0*0.4 + 0.0*0.4 + 1.0*0.2 = 0.6
    assertThat(score).isCloseTo(0.6, within(0.02));
}

@Test
void score_nullPlannedPaces_paceFactorDefaultsToOne() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, null, null, 1);
    Workout w = workout(user, 10000.0, 3150, 130);

    double score = scorer.score(w, pw);

    // Distance: 1.0*0.4, Pace: default 1.0*0.4, HR: 1.0*0.2 = 1.0
    assertThat(score).isCloseTo(1.0, within(0.01));
}

@Test
void score_zeroPaceRange_exactMatchIsOne() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.0, 1);
    // Exactly 5.0 min/km for 10km: 5.0 * 10 * 60 = 3000s
    Workout w = workout(user, 10000.0, 3000, 130);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.01));
}

@Test
void score_zeroPaceRange_anyDeviationIsZero() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.0, 1);
    // 5.5 min/km (off from exact 5.0)
    Workout w = workout(user, 10000.0, 3300, 130);

    double score = scorer.score(w, pw);

    // Pace factor = 0.0, total: 1.0*0.4 + 0.0*0.4 + 1.0*0.2 = 0.6
    assertThat(score).isCloseTo(0.6, within(0.02));
}
```

- [ ] **Step 5: Write HR zone tests**

Add to test class:

```java
@Test
void score_hrInTargetZone_fullHrFactor() {
    User user = user(180);
    // Zone 3 (Threshold): 86-90% of 180 = 155-162
    PlannedWorkout pw = planned(WorkoutType.THRESHOLD, 8000.0, 4.0, 4.3, 3);
    // Pace midpoint 4.15, for 8km: 4.15 * 8 * 60 = 1992s. avgHr 158 is in zone 3.
    Workout w = workout(user, 8000.0, 1992, 158);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.02));
}

@Test
void score_hrOneZoneOff_halfHrFactor() {
    User user = user(180);
    // Zone 3 (Threshold): 86-90% of 180 = 155-162
    PlannedWorkout pw = planned(WorkoutType.THRESHOLD, 8000.0, 4.0, 4.3, 3);
    // avgHr 150 is zone 2 (80-85% of 180 = 144-153), one zone below target zone 3
    Workout w = workout(user, 8000.0, 1992, 150);

    double score = scorer.score(w, pw);

    // Distance 1.0, pace ~1.0, HR 0.5: 0.4 + 0.4 + 0.1 = 0.9
    assertThat(score).isCloseTo(0.9, within(0.02));
}

@Test
void score_hrTwoZonesOff_zeroHrFactor() {
    User user = user(180);
    // Zone 3 target, avgHr 130 is zone 1 (65-79% of 180 = 117-142), two zones off
    PlannedWorkout pw = planned(WorkoutType.THRESHOLD, 8000.0, 4.0, 4.3, 3);
    Workout w = workout(user, 8000.0, 1992, 130);

    double score = scorer.score(w, pw);

    // HR factor = 0.0: 0.4 + 0.4 + 0.0 = 0.8
    assertThat(score).isCloseTo(0.8, within(0.02));
}

@Test
void score_nullAvgHr_hrFactorDefaultsToOne() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    Workout w = workout(user, 10000.0, 3150, null);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.01));
}

@Test
void score_nullUserMaxHr_hrFactorDefaultsToOne() {
    User user = user(null);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
    Workout w = workout(user, 10000.0, 3150, 130);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.01));
}

@Test
void score_nullPlannedHrZone_hrFactorDefaultsToOne() {
    User user = user(180);
    PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, null);
    Workout w = workout(user, 10000.0, 3150, 130);

    double score = scorer.score(w, pw);

    assertThat(score).isCloseTo(1.0, within(0.01));
}
```

- [ ] **Step 6: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=ComplianceScorerTest test`
Expected: COMPILATION FAILURE (ComplianceScorer does not exist yet)

- [ ] **Step 7: Commit**

```bash
git add src/test/java/com/runplanner/match/ComplianceScorerTest.java
git commit -m "test(match): add ComplianceScorer unit tests for all scoring factors"
```

---

### Task 5: ComplianceScorer — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/match/ComplianceScorer.java`

- [ ] **Step 1: Implement ComplianceScorer**

```java
package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.springframework.stereotype.Component;

import static com.runplanner.match.MatchConstants.*;

@Component
public class ComplianceScorer {

    public double score(Workout workout, PlannedWorkout plannedWorkout) {
        if (plannedWorkout.getWorkoutType() == WorkoutType.REST) {
            return 0.0;
        }

        double distanceFactor = calculateDistanceFactor(workout, plannedWorkout);
        double paceFactor = calculatePaceFactor(workout, plannedWorkout);
        double hrFactor = calculateHrFactor(workout, plannedWorkout);

        return distanceFactor * DISTANCE_WEIGHT
                + paceFactor * PACE_WEIGHT
                + hrFactor * HR_WEIGHT;
    }

    private double calculateDistanceFactor(Workout workout, PlannedWorkout planned) {
        if (planned.getTargetDistanceMeters() <= 0) {
            return DEFAULT_FACTOR;
        }
        return Math.min(workout.getDistanceMeters() / planned.getTargetDistanceMeters(), 1.0);
    }

    private double calculatePaceFactor(Workout workout, PlannedWorkout planned) {
        if (planned.getTargetPaceMinPerKm() == null || planned.getTargetPaceMaxPerKm() == null) {
            return DEFAULT_FACTOR;
        }

        double actualPace = (workout.getDurationSeconds() / 60.0)
                / (workout.getDistanceMeters() / 1000.0);
        double targetMid = (planned.getTargetPaceMinPerKm() + planned.getTargetPaceMaxPerKm()) / 2.0;
        double targetRange = planned.getTargetPaceMaxPerKm() - planned.getTargetPaceMinPerKm();

        if (targetRange == 0) {
            return actualPace == targetMid ? 1.0 : 0.0;
        }

        double deviation = Math.abs(actualPace - targetMid) / targetRange;
        return Math.max(0.0, 1.0 - Math.min(deviation, 1.0));
    }

    private double calculateHrFactor(Workout workout, PlannedWorkout planned) {
        if (planned.getTargetHrZone() == null || workout.getAvgHr() == null) {
            return DEFAULT_FACTOR;
        }

        User user = workout.getUser();
        if (user.getMaxHr() == null) {
            return DEFAULT_FACTOR;
        }

        int actualZone = determineHrZone(workout.getAvgHr(), user.getMaxHr());
        int targetZone = planned.getTargetHrZone();
        int zoneDiff = Math.abs(actualZone - targetZone);

        if (zoneDiff == 0) return HR_ZONE_MATCH;
        if (zoneDiff == 1) return HR_ZONE_ADJACENT;
        return HR_ZONE_FAR;
    }

    int determineHrZone(int avgHr, int maxHr) {
        double pctMaxHr = (double) avgHr / maxHr;
        for (int i = HR_ZONE_LOWER_BOUNDS.length - 1; i >= 0; i--) {
            if (pctMaxHr >= HR_ZONE_LOWER_BOUNDS[i]) {
                return i + 1; // zones are 1-indexed
            }
        }
        return 1; // below zone 1 lower bound, default to zone 1
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=ComplianceScorerTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/match/ComplianceScorer.java
git commit -m "feat(match): implement ComplianceScorer with 3-factor weighted scoring"
```

---

### Task 6: WorkoutMatcher — Tests

**Files:**
- Create: `src/test/java/com/runplanner/match/WorkoutMatcherTest.java`

- [ ] **Step 1: Write test class with helpers**

```java
package com.runplanner.match;

import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutMatcherTest {

    @Mock private WorkoutMatchRepository workoutMatchRepository;
    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private ComplianceScorer complianceScorer;

    @InjectMocks private WorkoutMatcher workoutMatcher;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").maxHr(180).build();
    }

    private Workout workout(User user, LocalDate date) {
        return Workout.builder()
                .id(UUID.randomUUID())
                .user(user)
                .source("TEST")
                .startedAt(date.atStartOfDay().toInstant(ZoneOffset.UTC))
                .distanceMeters(10000.0)
                .durationSeconds(3000)
                .avgHr(150)
                .build();
    }

    private TrainingPlan activePlan(User user) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(TrainingPlanStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    private PlannedWorkout planned(LocalDate date, WorkoutType type) {
        return PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .scheduledDate(date)
                .workoutType(type)
                .targetDistanceMeters(10000.0)
                .targetPaceMinPerKm(5.0)
                .targetPaceMaxPerKm(5.5)
                .originalScheduledDate(date)
                .build();
    }
}
```

- [ ] **Step 2: Write matching tests**

Add to test class:

```java
@Test
void match_happyPath_matchesBestCandidate() {
    User user = user();
    LocalDate date = LocalDate.of(2026, 3, 25);
    Workout w = workout(user, date);
    TrainingPlan plan = activePlan(user);
    PlannedWorkout pw = planned(date, WorkoutType.EASY);

    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.of(plan));
    when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
            .thenReturn(List.of(pw));
    when(workoutMatchRepository.existsByPlannedWorkout(pw)).thenReturn(false);
    when(complianceScorer.score(w, pw)).thenReturn(0.85);
    when(workoutMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isPresent();
    assertThat(result.get().getComplianceScore()).isEqualTo(0.85);
    verify(workoutMatchRepository).save(any());
}

@Test
void match_alreadyMatched_returnsEmpty() {
    User user = user();
    Workout w = workout(user, LocalDate.of(2026, 3, 25));
    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(true);

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isEmpty();
    verify(trainingPlanRepository, never()).findByUserAndStatus(any(), any());
}

@Test
void match_noActivePlan_returnsEmpty() {
    User user = user();
    Workout w = workout(user, LocalDate.of(2026, 3, 25));
    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.empty());

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isEmpty();
}

@Test
void match_noCandidatesWithinWindow_returnsEmpty() {
    User user = user();
    LocalDate workoutDate = LocalDate.of(2026, 3, 25);
    Workout w = workout(user, workoutDate);
    TrainingPlan plan = activePlan(user);
    // Planned workout is 3 days away — outside ±1 day window
    PlannedWorkout pw = planned(workoutDate.plusDays(3), WorkoutType.EASY);

    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.of(plan));
    when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
            .thenReturn(List.of(pw));

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isEmpty();
}

@Test
void match_multipleCandidates_picksBestScore() {
    User user = user();
    LocalDate date = LocalDate.of(2026, 3, 25);
    Workout w = workout(user, date);
    TrainingPlan plan = activePlan(user);
    PlannedWorkout pw1 = planned(date, WorkoutType.EASY);
    PlannedWorkout pw2 = planned(date, WorkoutType.THRESHOLD);

    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.of(plan));
    when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
            .thenReturn(List.of(pw1, pw2));
    when(workoutMatchRepository.existsByPlannedWorkout(pw1)).thenReturn(false);
    when(workoutMatchRepository.existsByPlannedWorkout(pw2)).thenReturn(false);
    when(complianceScorer.score(w, pw1)).thenReturn(0.7);
    when(complianceScorer.score(w, pw2)).thenReturn(0.9);
    when(workoutMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isPresent();
    assertThat(result.get().getPlannedWorkout()).isEqualTo(pw2);
    assertThat(result.get().getComplianceScore()).isEqualTo(0.9);
}

@Test
void match_restPlannedWorkoutsExcluded() {
    User user = user();
    LocalDate date = LocalDate.of(2026, 3, 25);
    Workout w = workout(user, date);
    TrainingPlan plan = activePlan(user);
    PlannedWorkout restPw = planned(date, WorkoutType.REST);

    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.of(plan));
    when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
            .thenReturn(List.of(restPw));

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isEmpty();
}

@Test
void match_alreadyMatchedPlannedWorkoutExcluded() {
    User user = user();
    LocalDate date = LocalDate.of(2026, 3, 25);
    Workout w = workout(user, date);
    TrainingPlan plan = activePlan(user);
    PlannedWorkout pw = planned(date, WorkoutType.EASY);

    when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.of(plan));
    when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
            .thenReturn(List.of(pw));
    when(workoutMatchRepository.existsByPlannedWorkout(pw)).thenReturn(true);

    Optional<WorkoutMatch> result = workoutMatcher.match(w);

    assertThat(result).isEmpty();
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=WorkoutMatcherTest test`
Expected: COMPILATION FAILURE (WorkoutMatcher does not exist yet)

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/runplanner/match/WorkoutMatcherTest.java
git commit -m "test(match): add WorkoutMatcher unit tests for matching algorithm"
```

---

### Task 7: WorkoutMatcher — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/match/WorkoutMatcher.java`

- [ ] **Step 1: Implement WorkoutMatcher**

```java
package com.runplanner.match;

import com.runplanner.plan.*;
import com.runplanner.workout.Workout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;

import static com.runplanner.match.MatchConstants.MATCH_WINDOW_DAYS;

@Service
@RequiredArgsConstructor
public class WorkoutMatcher {

    private final WorkoutMatchRepository workoutMatchRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final ComplianceScorer complianceScorer;

    @Transactional
    public Optional<WorkoutMatch> match(Workout workout) {
        if (workoutMatchRepository.existsByWorkout(workout)) {
            return Optional.empty();
        }

        var activePlan = trainingPlanRepository.findByUserAndStatus(
                workout.getUser(), TrainingPlanStatus.ACTIVE);
        if (activePlan.isEmpty()) {
            return Optional.empty();
        }

        LocalDate workoutDate = workout.getStartedAt()
                .atZone(ZoneOffset.UTC).toLocalDate();

        var allPlanned = plannedWorkoutRepository
                .findAllByTrainingPlanOrderByScheduledDate(activePlan.get());

        record ScoredCandidate(PlannedWorkout planned, double score) {}

        var bestCandidate = allPlanned.stream()
                .filter(pw -> pw.getWorkoutType() != WorkoutType.REST)
                .filter(pw -> !workoutMatchRepository.existsByPlannedWorkout(pw))
                .filter(pw -> isWithinWindow(workoutDate, pw.getScheduledDate()))
                .map(pw -> new ScoredCandidate(pw, complianceScorer.score(workout, pw)))
                .max(Comparator.comparingDouble(ScoredCandidate::score));

        if (bestCandidate.isEmpty()) {
            return Optional.empty();
        }

        var match = WorkoutMatch.builder()
                .plannedWorkout(bestCandidate.get().planned())
                .workout(workout)
                .complianceScore(bestCandidate.get().score())
                .build();

        return Optional.of(workoutMatchRepository.save(match));
    }

    private boolean isWithinWindow(LocalDate workoutDate, LocalDate scheduledDate) {
        long daysBetween = Math.abs(workoutDate.toEpochDay() - scheduledDate.toEpochDay());
        return daysBetween <= MATCH_WINDOW_DAYS;
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=WorkoutMatcherTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/match/WorkoutMatcher.java
git commit -m "feat(match): implement WorkoutMatcher with ±1 day window and best-score selection"
```

---

### Task 8: Full Test Suite Verification

- [ ] **Step 1: Run all tests**

Run: `cd run-planner-backend && mvn test`
Expected: All tests PASS (including all existing auth, goalrace, vdot, workout, plan, and new match tests)

- [ ] **Step 2: Verify build**

Run: `cd run-planner-backend && mvn clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 3: Final commit if any issues found**

If the full build reveals any issues, fix and commit:

```bash
git add -A
git commit -m "chore: fix issues from full build verification"
```
