# Training Plans Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement training plan generation using Daniels periodization, producing day-by-day plans with VDOT-derived paces, plus REST endpoints for plan management.

**Architecture:** Two entities (`TrainingPlan`, `PlannedWorkout`) in a `plan` package. A `TrainingPlanGenerator` contains the pure Daniels periodization logic — phase assignment, weekly templates, mileage progression, distance distribution. `TrainingPlanService` orchestrates generation and CRUD. `TrainingPlanController` exposes RESTful endpoints. Flyway V6/V7 create the tables.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL, Flyway, JUnit 5, Mockito, AssertJ, MockMvc, Lombok

**Spec:** `docs/superpowers/specs/2026-03-25-training-plans-design.md`

---

### Task 1: Database Migrations (V6 + V7)

**Files:**
- Create: `run-planner-backend/src/main/resources/db/migration/V6__create_training_plans.sql`
- Create: `run-planner-backend/src/main/resources/db/migration/V7__create_planned_workouts.sql`

- [ ] **Step 1: Create V6 migration**

```sql
CREATE TABLE training_plans (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_race_id  UUID NOT NULL REFERENCES goal_races(id) ON DELETE CASCADE,
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    revision      INTEGER NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_training_plans_user_id ON training_plans(user_id);
CREATE INDEX idx_training_plans_goal_race_id ON training_plans(goal_race_id);
```

- [ ] **Step 2: Create V7 migration**

```sql
CREATE TABLE planned_workouts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_plan_id        UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    week_number             INTEGER NOT NULL,
    day_of_week             INTEGER NOT NULL,
    scheduled_date          DATE NOT NULL,
    workout_type            VARCHAR(20) NOT NULL,
    target_distance_meters  DOUBLE PRECISION NOT NULL,
    target_pace_min_per_km  DOUBLE PRECISION,
    target_pace_max_per_km  DOUBLE PRECISION,
    target_hr_zone          INTEGER,
    notes                   TEXT,
    original_scheduled_date DATE NOT NULL,
    plan_revision           INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_planned_workouts_plan_id ON planned_workouts(training_plan_id);
CREATE INDEX idx_planned_workouts_scheduled_date ON planned_workouts(scheduled_date);
```

- [ ] **Step 3: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V6__create_training_plans.sql \
       src/main/resources/db/migration/V7__create_planned_workouts.sql
git commit -m "feat(plan): add V6/V7 migrations for training_plans and planned_workouts"
```

---

### Task 2: Enums, Entities, and Repositories

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/TrainingPlanStatus.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/WorkoutType.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/TrainingPlan.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/PlannedWorkout.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/TrainingPlanRepository.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/PlannedWorkoutRepository.java`

- [ ] **Step 1: Create TrainingPlanStatus enum**

```java
package com.runplanner.plan;

public enum TrainingPlanStatus {
    ACTIVE, COMPLETED, ARCHIVED
}
```

- [ ] **Step 2: Create WorkoutType enum**

```java
package com.runplanner.plan;

import com.runplanner.vdot.TrainingZone;

public enum WorkoutType {

    EASY(TrainingZone.E),
    LONG(TrainingZone.E),
    MARATHON(TrainingZone.M),
    THRESHOLD(TrainingZone.T),
    INTERVAL(TrainingZone.I),
    REPETITION(TrainingZone.R),
    REST(null);

    private final TrainingZone trainingZone;

    WorkoutType(TrainingZone trainingZone) {
        this.trainingZone = trainingZone;
    }

    public TrainingZone getTrainingZone() {
        return trainingZone;
    }
}
```

- [ ] **Step 3: Create TrainingPlan entity**

```java
package com.runplanner.plan;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_race_id", nullable = false)
    private GoalRace goalRace;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrainingPlanStatus status = TrainingPlanStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private int revision = 1;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
```

- [ ] **Step 4: Create PlannedWorkout entity**

```java
package com.runplanner.plan;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "planned_workouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @Column(nullable = false)
    private int weekNumber;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkoutType workoutType;

    @Column(nullable = false)
    private double targetDistanceMeters;

    private Double targetPaceMinPerKm;

    private Double targetPaceMaxPerKm;

    private Integer targetHrZone;

    private String notes;

    @Column(nullable = false)
    private LocalDate originalScheduledDate;

    @Column(nullable = false)
    @Builder.Default
    private int planRevision = 1;
}
```

- [ ] **Step 5: Create TrainingPlanRepository**

```java
package com.runplanner.plan;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

    Optional<TrainingPlan> findByUserAndStatus(User user, TrainingPlanStatus status);

    List<TrainingPlan> findAllByUserOrderByCreatedAtDesc(User user);

    Optional<TrainingPlan> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndStatus(User user, TrainingPlanStatus status);
}
```

- [ ] **Step 6: Create PlannedWorkoutRepository**

```java
package com.runplanner.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlannedWorkoutRepository extends JpaRepository<PlannedWorkout, UUID> {

    List<PlannedWorkout> findAllByTrainingPlanOrderByScheduledDate(TrainingPlan trainingPlan);

    List<PlannedWorkout> findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
            TrainingPlan trainingPlan, LocalDate from, LocalDate to);
}
```

- [ ] **Step 7: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/runplanner/plan/
git commit -m "feat(plan): add TrainingPlan and PlannedWorkout entities, enums, and repositories"
```

---

### Task 3: Cross-Cutting Change — @Component on TrainingPaceCalculator

**Files:**
- Modify: `run-planner-backend/src/main/java/com/runplanner/vdot/TrainingPaceCalculator.java`

- [ ] **Step 1: Add @Component to TrainingPaceCalculator**

Add `import org.springframework.stereotype.Component;` and `@Component` annotation:

```java
package com.runplanner.vdot;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

import static com.runplanner.vdot.VdotConstants.*;

@Component
public class TrainingPaceCalculator {
```

- [ ] **Step 2: Verify compilation and existing tests pass**

Run: `cd run-planner-backend && mvn test -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/vdot/TrainingPaceCalculator.java
git commit -m "feat(vdot): add @Component to TrainingPaceCalculator for injection"
```

---

### Task 4: PlanConstants

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/PlanConstants.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/plan/PlanConstantsTest.java`

- [ ] **Step 1: Write PlanConstants tests**

```java
package com.runplanner.plan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlanConstantsTest {

    @Test
    void phasePercentages_sumToOne() {
        double sum = PlanConstants.BASE_PHASE_PCT + PlanConstants.QUALITY_PHASE_PCT
                + PlanConstants.PEAK_PHASE_PCT + PlanConstants.TAPER_PHASE_PCT;
        assertThat(sum).isCloseTo(1.0, within(0.001));
    }

    @Test
    void weeklyTemplates_eachHaveSevenDays() {
        for (Map.Entry<TrainingPhase, WorkoutType[]> entry : PlanConstants.WEEKLY_TEMPLATES.entrySet()) {
            assertThat(entry.getValue()).hasSize(7)
                    .as("Template for %s should have 7 days", entry.getKey());
        }
    }

    @Test
    void weeklyTemplates_coverAllPhases() {
        assertThat(PlanConstants.WEEKLY_TEMPLATES).containsKeys(TrainingPhase.values());
    }

    @Test
    void distanceDistribution_longRunProportion_isValid() {
        assertThat(PlanConstants.LONG_RUN_PCT).isBetween(0.25, 0.30);
    }

    @Test
    void distanceDistribution_qualityProportion_isValid() {
        assertThat(PlanConstants.QUALITY_SESSION_PCT).isBetween(0.15, 0.20);
    }
}
```

- [ ] **Step 2: Create TrainingPhase enum** (internal to plan package, used by constants and generator)

```java
package com.runplanner.plan;

enum TrainingPhase {
    BASE, QUALITY, PEAK, TAPER
}
```

- [ ] **Step 3: Create PlanConstants**

```java
package com.runplanner.plan;

import java.util.EnumMap;
import java.util.Map;

public final class PlanConstants {

    private PlanConstants() {}

    // Phase splits (must sum to 1.0)
    public static final double BASE_PHASE_PCT = 0.25;
    public static final double QUALITY_PHASE_PCT = 0.25;
    public static final double PEAK_PHASE_PCT = 0.25;
    public static final double TAPER_PHASE_PCT = 0.25;

    // Minimum weeks per phase for compressed plans
    public static final int MIN_WEEKS_PER_PHASE = 1;

    // Minimum total weeks for a 4-phase plan
    public static final int MIN_WEEKS_FULL_PLAN = 4;

    // Weekly templates: index 0=Monday(1) through 6=Sunday(7)
    public static final Map<TrainingPhase, WorkoutType[]> WEEKLY_TEMPLATES;

    static {
        WEEKLY_TEMPLATES = new EnumMap<>(TrainingPhase.class);
        WEEKLY_TEMPLATES.put(TrainingPhase.BASE, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.EASY, WorkoutType.EASY,
                WorkoutType.EASY, WorkoutType.REST, WorkoutType.LONG, WorkoutType.EASY
        });
        WEEKLY_TEMPLATES.put(TrainingPhase.QUALITY, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.THRESHOLD, WorkoutType.EASY,
                WorkoutType.INTERVAL, WorkoutType.REST, WorkoutType.LONG, WorkoutType.EASY
        });
        WEEKLY_TEMPLATES.put(TrainingPhase.PEAK, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.INTERVAL, WorkoutType.EASY,
                WorkoutType.THRESHOLD, WorkoutType.REST, WorkoutType.LONG, WorkoutType.MARATHON
        });
        WEEKLY_TEMPLATES.put(TrainingPhase.TAPER, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.EASY, WorkoutType.EASY,
                WorkoutType.THRESHOLD, WorkoutType.REST, WorkoutType.LONG, WorkoutType.EASY
        });
    }

    // Distance distribution within a week
    public static final double LONG_RUN_PCT = 0.27;
    public static final double QUALITY_SESSION_PCT = 0.17;
    public static final double REST_DISTANCE = 0.0;

    // Mileage progression factors (multiplied against weekly target)
    public static final double BASE_START_FACTOR = 0.70;
    public static final double BASE_END_FACTOR = 1.00;
    public static final double QUALITY_FACTOR = 1.00;
    public static final double PEAK_START_FACTOR = 1.00;
    public static final double PEAK_END_FACTOR = 1.05;
    public static final double TAPER_START_FACTOR = 1.00;
    public static final double TAPER_END_FACTOR = 0.60;

    // Base weekly mileage (km) by VDOT range
    // Linear interpolation: baseKm = MILEAGE_BASE + (vdot - 30) * MILEAGE_PER_VDOT
    public static final double MILEAGE_BASE_KM = 30.0;    // km/week at VDOT 30
    public static final double MILEAGE_PER_VDOT = 1.0;    // additional km per VDOT point
}
```

- [ ] **Step 4: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=PlanConstantsTest test`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/runplanner/plan/TrainingPhase.java \
       src/main/java/com/runplanner/plan/PlanConstants.java \
       src/test/java/com/runplanner/plan/PlanConstantsTest.java
git commit -m "feat(plan): add PlanConstants and TrainingPhase with validation tests"
```

---

### Task 5: TrainingPlanGenerator — Tests

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/plan/TrainingPlanGeneratorTest.java`

- [ ] **Step 1: Write test class with helpers**

```java
package com.runplanner.plan;

import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingPlanGeneratorTest {

    @Mock private TrainingPaceCalculator trainingPaceCalculator;

    @InjectMocks private TrainingPlanGenerator generator;

    private static final double VDOT = 50.0;
    private static final int RACE_DISTANCE_METERS = 21097; // half marathon

    private Map<TrainingZone, PaceRange> samplePaces() {
        return Map.of(
                TrainingZone.E, new PaceRange(5.0, 6.0),
                TrainingZone.M, new PaceRange(4.5, 4.7),
                TrainingZone.T, new PaceRange(4.0, 4.3),
                TrainingZone.I, new PaceRange(3.5, 3.8),
                TrainingZone.R, new PaceRange(3.0, 3.3)
        );
    }

    private void stubPaces() {
        when(trainingPaceCalculator.calculate(anyDouble())).thenReturn(samplePaces());
    }
}
```

- [ ] **Step 2: Write week count and date tests**

Add to test class:

```java
@Test
void generate_12weeks_produces84days() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5); // Monday
    LocalDate raceDate = LocalDate.of(2026, 3, 30); // Monday, 12 weeks later

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    assertThat(workouts).hasSize(84); // 12 weeks * 7 days
}

@Test
void generate_allDatesAreWithinStartAndEndRange() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.forEach(w -> {
        assertThat(w.getScheduledDate()).isAfterOrEqualTo(start);
        assertThat(w.getScheduledDate()).isBefore(raceDate);
    });
}

@Test
void generate_originalScheduledDate_equalsScheduledDate() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.forEach(w ->
            assertThat(w.getOriginalScheduledDate()).isEqualTo(w.getScheduledDate()));
}

@Test
void generate_allPlanRevisions_areOne() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.forEach(w -> assertThat(w.getPlanRevision()).isEqualTo(1));
}

@Test
void generate_dayOfWeek_matchesScheduledDate() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.forEach(w ->
            assertThat(w.getDayOfWeek()).isEqualTo(w.getScheduledDate().getDayOfWeek().getValue()));
}
```

- [ ] **Step 3: Write phase assignment tests**

Add to test class:

```java
@Test
void generate_12weeks_assignsPhasesEvenly() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    // 12 weeks / 4 phases = 3 weeks each
    // Week 1-3: base (days 0-20)
    // Check that week 1 Tuesday (index 1) is EASY (base template)
    PlannedWorkout week1Tue = workouts.get(1); // Tuesday of week 1
    assertThat(week1Tue.getWorkoutType()).isEqualTo(WorkoutType.EASY);

    // Week 4-6: quality - Tuesday should be THRESHOLD
    PlannedWorkout week4Tue = workouts.get(3 * 7 + 1); // Tuesday of week 4
    assertThat(week4Tue.getWorkoutType()).isEqualTo(WorkoutType.THRESHOLD);

    // Week 7-9: peak - Tuesday should be INTERVAL
    PlannedWorkout week7Tue = workouts.get(6 * 7 + 1); // Tuesday of week 7
    assertThat(week7Tue.getWorkoutType()).isEqualTo(WorkoutType.INTERVAL);

    // Week 10-12: taper - Tuesday should be EASY
    PlannedWorkout week10Tue = workouts.get(9 * 7 + 1); // Tuesday of week 10
    assertThat(week10Tue.getWorkoutType()).isEqualTo(WorkoutType.EASY);
}

@Test
void generate_shortPlan_3weeks_skipBaseAndQuality() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 3, 9); // Monday
    LocalDate raceDate = LocalDate.of(2026, 3, 30); // 3 weeks

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    assertThat(workouts).hasSize(21); // 3 weeks * 7 days

    // Should be peak (weeks 1-2ish) + taper (last week-ish)
    // Week 1 Tuesday should be INTERVAL (peak template)
    PlannedWorkout week1Tue = workouts.get(1);
    assertThat(week1Tue.getWorkoutType()).isEqualTo(WorkoutType.INTERVAL);
}
```

- [ ] **Step 4: Write weekly template tests**

Add to test class:

```java
@Test
void generate_mondaysAndFridays_areRestDays() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.stream()
            .filter(w -> w.getDayOfWeek() == 1 || w.getDayOfWeek() == 5) // Mon, Fri
            .forEach(w -> assertThat(w.getWorkoutType()).isEqualTo(WorkoutType.REST));
}

@Test
void generate_saturdays_areLongRuns() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.stream()
            .filter(w -> w.getDayOfWeek() == 6) // Saturday
            .forEach(w -> assertThat(w.getWorkoutType()).isEqualTo(WorkoutType.LONG));
}
```

- [ ] **Step 5: Write distance and pace tests**

Add to test class:

```java
@Test
void generate_restDays_haveZeroDistanceAndNullPaces() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.stream()
            .filter(w -> w.getWorkoutType() == WorkoutType.REST)
            .forEach(w -> {
                assertThat(w.getTargetDistanceMeters()).isEqualTo(0.0);
                assertThat(w.getTargetPaceMinPerKm()).isNull();
                assertThat(w.getTargetPaceMaxPerKm()).isNull();
            });
}

@Test
void generate_nonRestDays_havePositiveDistanceAndPaces() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.stream()
            .filter(w -> w.getWorkoutType() != WorkoutType.REST)
            .forEach(w -> {
                assertThat(w.getTargetDistanceMeters()).isGreaterThan(0);
                assertThat(w.getTargetPaceMinPerKm()).isNotNull();
                assertThat(w.getTargetPaceMaxPerKm()).isNotNull();
            });
}

@Test
void generate_longRun_getsLargestDistanceInWeek() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    // Check first week: long run (Saturday) should be the largest distance
    List<PlannedWorkout> week1 = workouts.subList(0, 7);
    PlannedWorkout longRun = week1.stream()
            .filter(w -> w.getWorkoutType() == WorkoutType.LONG)
            .findFirst().orElseThrow();
    double maxNonLong = week1.stream()
            .filter(w -> w.getWorkoutType() != WorkoutType.LONG && w.getWorkoutType() != WorkoutType.REST)
            .mapToDouble(PlannedWorkout::getTargetDistanceMeters)
            .max().orElse(0);

    assertThat(longRun.getTargetDistanceMeters()).isGreaterThan(maxNonLong);
}

@Test
void generate_taperWeeks_haveLowerTotalDistanceThanPeakWeeks() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    // 12 weeks: peak weeks 7-9 (index 42-62), taper weeks 10-12 (index 63-83)
    double peakTotal = workouts.subList(42, 63).stream()
            .mapToDouble(PlannedWorkout::getTargetDistanceMeters).sum();
    double taperTotal = workouts.subList(63, 84).stream()
            .mapToDouble(PlannedWorkout::getTargetDistanceMeters).sum();

    assertThat(taperTotal).isLessThan(peakTotal);
}

@Test
void generate_weekNumbers_areSequential() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    for (int i = 0; i < workouts.size(); i++) {
        int expectedWeek = (i / 7) + 1;
        assertThat(workouts.get(i).getWeekNumber()).isEqualTo(expectedWeek);
    }
}

@Test
void generate_trainingPlan_isNull() {
    stubPaces();
    LocalDate start = LocalDate.of(2026, 1, 5);
    LocalDate raceDate = LocalDate.of(2026, 3, 30);

    List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

    workouts.forEach(w -> assertThat(w.getTrainingPlan()).isNull());
}
```

- [ ] **Step 6: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=TrainingPlanGeneratorTest test`
Expected: COMPILATION FAILURE (TrainingPlanGenerator does not exist yet)

- [ ] **Step 7: Commit**

```bash
git add src/test/java/com/runplanner/plan/TrainingPlanGeneratorTest.java
git commit -m "test(plan): add TrainingPlanGenerator tests for periodization logic"
```

---

### Task 6: TrainingPlanGenerator — Implementation

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/TrainingPlanGenerator.java`

- [ ] **Step 1: Implement TrainingPlanGenerator**

```java
package com.runplanner.plan;

import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.runplanner.plan.PlanConstants.*;

@Component
@RequiredArgsConstructor
public class TrainingPlanGenerator {

    private final TrainingPaceCalculator trainingPaceCalculator;

    public List<PlannedWorkout> generate(double vdot, int raceDistanceMeters,
                                          LocalDate raceDate, LocalDate startDate) {
        int totalWeeks = (int) ChronoUnit.WEEKS.between(startDate, raceDate);
        if (totalWeeks <= 0) {
            throw new IllegalArgumentException("Race date must be after start date by at least one week");
        }

        Map<TrainingZone, PaceRange> paces = trainingPaceCalculator.calculate(vdot);
        double weeklyTargetKm = MILEAGE_BASE_KM + (vdot - 30.0) * MILEAGE_PER_VDOT;
        int[] phaseLengths = calculatePhaseLengths(totalWeeks);

        List<PlannedWorkout> workouts = new ArrayList<>();
        int weekNumber = 1;

        for (int phaseIdx = 0; phaseIdx < phaseLengths.length; phaseIdx++) {
            TrainingPhase phase = TrainingPhase.values()[phaseIdx];
            int phaseWeeks = phaseLengths[phaseIdx];

            for (int weekInPhase = 0; weekInPhase < phaseWeeks; weekInPhase++) {
                double mileageFactor = calculateMileageFactor(phase, weekInPhase, phaseWeeks);
                double weekDistanceKm = weeklyTargetKm * mileageFactor;
                WorkoutType[] template = WEEKLY_TEMPLATES.get(phase);

                for (int day = 0; day < 7; day++) {
                    LocalDate date = startDate.plusWeeks(weekNumber - 1).plusDays(day);
                    WorkoutType type = template[day];
                    double distanceMeters = calculateDayDistance(type, template, weekDistanceKm) * 1000.0;

                    PaceRange pace = null;
                    if (type.getTrainingZone() != null) {
                        pace = paces.get(type.getTrainingZone());
                    }

                    workouts.add(PlannedWorkout.builder()
                            .weekNumber(weekNumber)
                            .dayOfWeek(date.getDayOfWeek().getValue())
                            .scheduledDate(date)
                            .workoutType(type)
                            .targetDistanceMeters(distanceMeters)
                            .targetPaceMinPerKm(pace != null ? pace.minPaceMinPerKm() : null)
                            .targetPaceMaxPerKm(pace != null ? pace.maxPaceMinPerKm() : null)
                            .originalScheduledDate(date)
                            .planRevision(1)
                            .build());
                }
                weekNumber++;
            }
        }

        return workouts;
    }

    int[] calculatePhaseLengths(int totalWeeks) {
        if (totalWeeks < MIN_WEEKS_FULL_PLAN) {
            // Short plan: peak + taper only
            int taper = Math.max(1, totalWeeks / 2);
            int peak = totalWeeks - taper;
            return new int[]{0, 0, peak, taper};
        }

        int base = Math.max(MIN_WEEKS_PER_PHASE, (int) Math.round(totalWeeks * BASE_PHASE_PCT));
        int quality = Math.max(MIN_WEEKS_PER_PHASE, (int) Math.round(totalWeeks * QUALITY_PHASE_PCT));
        int peak = Math.max(MIN_WEEKS_PER_PHASE, (int) Math.round(totalWeeks * PEAK_PHASE_PCT));
        int taper = totalWeeks - base - quality - peak; // remainder goes to taper
        taper = Math.max(MIN_WEEKS_PER_PHASE, taper);

        // Adjust if rounding caused overflow
        int total = base + quality + peak + taper;
        if (total > totalWeeks) {
            taper -= (total - totalWeeks);
        } else if (total < totalWeeks) {
            base += (totalWeeks - total);
        }

        return new int[]{base, quality, peak, taper};
    }

    private double calculateMileageFactor(TrainingPhase phase, int weekInPhase, int phaseWeeks) {
        double progress = phaseWeeks > 1 ? (double) weekInPhase / (phaseWeeks - 1) : 0.0;
        return switch (phase) {
            case BASE -> BASE_START_FACTOR + progress * (BASE_END_FACTOR - BASE_START_FACTOR);
            case QUALITY -> QUALITY_FACTOR;
            case PEAK -> PEAK_START_FACTOR + progress * (PEAK_END_FACTOR - PEAK_START_FACTOR);
            case TAPER -> TAPER_START_FACTOR + progress * (TAPER_END_FACTOR - TAPER_START_FACTOR);
        };
    }

    private double calculateDayDistance(WorkoutType type, WorkoutType[] template, double weekDistanceKm) {
        if (type == WorkoutType.REST) {
            return REST_DISTANCE;
        }

        // Count run days and categorize
        int easyCount = 0;
        boolean hasLong = false;
        int qualityCount = 0;

        for (WorkoutType t : template) {
            switch (t) {
                case REST -> {}
                case LONG -> hasLong = true;
                case EASY -> easyCount++;
                default -> qualityCount++;
            }
        }

        double remaining = weekDistanceKm;
        double longDistance = hasLong ? weekDistanceKm * LONG_RUN_PCT : 0;
        remaining -= longDistance;

        double qualityDistance = qualityCount > 0 ? weekDistanceKm * QUALITY_SESSION_PCT : 0;
        double totalQuality = qualityDistance * qualityCount;
        remaining -= totalQuality;

        double easyDistance = easyCount > 0 ? remaining / easyCount : 0;

        return switch (type) {
            case LONG -> longDistance;
            case EASY -> easyDistance;
            case REST -> REST_DISTANCE;
            default -> qualityDistance; // THRESHOLD, INTERVAL, MARATHON, REPETITION
        };
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=TrainingPlanGeneratorTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/plan/TrainingPlanGenerator.java
git commit -m "feat(plan): implement TrainingPlanGenerator with Daniels periodization"
```

---

### Task 7: DTOs

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/dto/CreatePlanRequest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/dto/PlannedWorkoutResponse.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/dto/TrainingPlanResponse.java`

- [ ] **Step 1: Create CreatePlanRequest**

```java
package com.runplanner.plan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePlanRequest(@NotNull UUID goalRaceId) {}
```

- [ ] **Step 2: Create PlannedWorkoutResponse**

```java
package com.runplanner.plan.dto;

import com.runplanner.plan.PlannedWorkout;

import java.time.LocalDate;
import java.util.UUID;

public record PlannedWorkoutResponse(
        UUID id,
        int weekNumber,
        int dayOfWeek,
        LocalDate scheduledDate,
        String workoutType,
        double targetDistanceMeters,
        Double targetPaceMinPerKm,
        Double targetPaceMaxPerKm,
        Integer targetHrZone,
        String notes,
        int planRevision
) {
    public static PlannedWorkoutResponse from(PlannedWorkout pw) {
        return new PlannedWorkoutResponse(
                pw.getId(),
                pw.getWeekNumber(),
                pw.getDayOfWeek(),
                pw.getScheduledDate(),
                pw.getWorkoutType().name(),
                pw.getTargetDistanceMeters(),
                pw.getTargetPaceMinPerKm(),
                pw.getTargetPaceMaxPerKm(),
                pw.getTargetHrZone(),
                pw.getNotes(),
                pw.getPlanRevision()
        );
    }
}
```

- [ ] **Step 3: Create TrainingPlanResponse**

```java
package com.runplanner.plan.dto;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.TrainingPlan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrainingPlanResponse(
        UUID id,
        UUID goalRaceId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        int revision,
        Instant createdAt,
        List<PlannedWorkoutResponse> workouts
) {
    public static TrainingPlanResponse from(TrainingPlan plan) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getGoalRace().getId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus().name(),
                plan.getRevision(),
                plan.getCreatedAt(),
                null
        );
    }

    public static TrainingPlanResponse from(TrainingPlan plan, List<PlannedWorkout> workouts) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getGoalRace().getId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus().name(),
                plan.getRevision(),
                plan.getCreatedAt(),
                workouts.stream().map(PlannedWorkoutResponse::from).toList()
        );
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd run-planner-backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/runplanner/plan/dto/
git commit -m "feat(plan): add DTOs for training plan REST endpoints"
```

---

### Task 8: TrainingPlanService — Tests

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/plan/TrainingPlanServiceTest.java`

- [ ] **Step 1: Write test class with helpers**

```java
package com.runplanner.plan;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceRepository;
import com.runplanner.goalrace.GoalRaceStatus;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private TrainingPlanGenerator trainingPlanGenerator;
    @Mock private GoalRaceRepository goalRaceRepository;
    @Mock private VdotHistoryService vdotHistoryService;

    @InjectMocks private TrainingPlanService service;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").build();
    }

    private GoalRace goalRace(User user) {
        return GoalRace.builder()
                .id(UUID.randomUUID())
                .user(user)
                .distanceMeters(21097)
                .distanceLabel("Half Marathon")
                .raceDate(LocalDate.of(2026, 6, 1))
                .status(GoalRaceStatus.ACTIVE)
                .build();
    }
}
```

- [ ] **Step 2: Write generate tests**

Add to test class:

```java
@Test
void generate_happyPath_createsPlanAndWorkouts() {
    User user = user();
    GoalRace race = goalRace(user);
    when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
    when(trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)).thenReturn(false);
    when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
    when(trainingPlanRepository.save(any())).thenAnswer(inv -> {
        TrainingPlan p = inv.getArgument(0);
        p.setId(UUID.randomUUID());
        return p;
    });
    when(trainingPlanGenerator.generate(eq(50.0), eq(21097), eq(race.getRaceDate()), any()))
            .thenReturn(List.of(PlannedWorkout.builder().weekNumber(1).dayOfWeek(1)
                    .scheduledDate(LocalDate.now()).workoutType(WorkoutType.REST)
                    .targetDistanceMeters(0).originalScheduledDate(LocalDate.now()).build()));
    when(plannedWorkoutRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    TrainingPlanResponse result = service.generate(user, race.getId());

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("ACTIVE");
    verify(trainingPlanRepository).save(any());
    verify(plannedWorkoutRepository).saveAll(anyList());
}

@Test
void generate_activePlanExists_throws409() {
    User user = user();
    GoalRace race = goalRace(user);
    when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
    when(trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)).thenReturn(true);

    assertThatThrownBy(() -> service.generate(user, race.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("active plan");
}

@Test
void generate_noVdot_throws400() {
    User user = user();
    GoalRace race = goalRace(user);
    when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
    when(trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)).thenReturn(false);
    when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.generate(user, race.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("VDOT");
}

@Test
void generate_goalRaceNotFound_throws404() {
    User user = user();
    UUID raceId = UUID.randomUUID();
    when(goalRaceRepository.findByIdAndUser(raceId, user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.generate(user, raceId))
            .isInstanceOf(ResponseStatusException.class);
}
```

- [ ] **Step 3: Write read and archive tests**

Add to test class:

```java
@Test
void findActive_withActivePlan_returnsPlanWithWorkouts() {
    User user = user();
    TrainingPlan plan = TrainingPlan.builder().id(UUID.randomUUID()).user(user)
            .goalRace(goalRace(user)).startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(12))
            .status(TrainingPlanStatus.ACTIVE).build();
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.of(plan));
    when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
            .thenReturn(List.of());

    TrainingPlanResponse result = service.findActive(user);

    assertThat(result).isNotNull();
    assertThat(result.workouts()).isNotNull();
}

@Test
void findActive_noActivePlan_throws404() {
    User user = user();
    when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findActive(user))
            .isInstanceOf(ResponseStatusException.class);
}

@Test
void findById_foreignPlan_throws404() {
    User user = user();
    UUID planId = UUID.randomUUID();
    when(trainingPlanRepository.findByIdAndUser(planId, user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(user, planId))
            .isInstanceOf(ResponseStatusException.class);
}

@Test
void archive_activePlan_setsStatusArchived() {
    User user = user();
    TrainingPlan plan = TrainingPlan.builder().id(UUID.randomUUID()).user(user)
            .goalRace(goalRace(user)).status(TrainingPlanStatus.ACTIVE).build();
    when(trainingPlanRepository.findByIdAndUser(plan.getId(), user)).thenReturn(Optional.of(plan));
    when(trainingPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.archive(user, plan.getId());

    ArgumentCaptor<TrainingPlan> captor = ArgumentCaptor.forClass(TrainingPlan.class);
    verify(trainingPlanRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(TrainingPlanStatus.ARCHIVED);
}

@Test
void archive_nonActivePlan_throws400() {
    User user = user();
    TrainingPlan plan = TrainingPlan.builder().id(UUID.randomUUID()).user(user)
            .goalRace(goalRace(user)).status(TrainingPlanStatus.ARCHIVED).build();
    when(trainingPlanRepository.findByIdAndUser(plan.getId(), user)).thenReturn(Optional.of(plan));

    assertThatThrownBy(() -> service.archive(user, plan.getId()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("ACTIVE");
}
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=TrainingPlanServiceTest test`
Expected: COMPILATION FAILURE (TrainingPlanService does not exist yet)

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/runplanner/plan/TrainingPlanServiceTest.java
git commit -m "test(plan): add TrainingPlanService unit tests"
```

---

### Task 9: TrainingPlanService — Implementation

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/TrainingPlanService.java`

- [ ] **Step 1: Implement TrainingPlanService**

```java
package com.runplanner.plan;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceRepository;
import com.runplanner.plan.dto.PlannedWorkoutResponse;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final TrainingPlanGenerator trainingPlanGenerator;
    private final GoalRaceRepository goalRaceRepository;
    private final VdotHistoryService vdotHistoryService;

    @Transactional
    public TrainingPlanResponse generate(User user, UUID goalRaceId) {
        GoalRace race = goalRaceRepository.findByIdAndUser(goalRaceId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Goal race not found"));

        if (trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active plan already exists. Archive it before creating a new one.");
        }

        double vdot = vdotHistoryService.getEffectiveVdot(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No VDOT score available. Complete a qualifying workout first."));

        LocalDate startDate = LocalDate.now();
        TrainingPlan plan = trainingPlanRepository.save(TrainingPlan.builder()
                .user(user)
                .goalRace(race)
                .startDate(startDate)
                .endDate(race.getRaceDate())
                .build());

        List<PlannedWorkout> workouts = trainingPlanGenerator.generate(
                vdot, race.getDistanceMeters(), race.getRaceDate(), startDate);

        workouts.forEach(w -> w.setTrainingPlan(plan));
        plannedWorkoutRepository.saveAll(workouts);

        return TrainingPlanResponse.from(plan, workouts);
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse findActive(User user) {
        TrainingPlan plan = trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active training plan"));
        List<PlannedWorkout> workouts =
                plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan);
        return TrainingPlanResponse.from(plan, workouts);
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse findById(User user, UUID planId) {
        TrainingPlan plan = findOwnedPlan(user, planId);
        List<PlannedWorkout> workouts =
                plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan);
        return TrainingPlanResponse.from(plan, workouts);
    }

    @Transactional(readOnly = true)
    public List<TrainingPlanResponse> findAll(User user) {
        return trainingPlanRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(TrainingPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlannedWorkoutResponse> findPlannedWorkouts(User user, UUID planId,
                                                             LocalDate from, LocalDate to) {
        TrainingPlan plan = findOwnedPlan(user, planId);
        List<PlannedWorkout> workouts;
        if (from != null && to != null) {
            workouts = plannedWorkoutRepository
                    .findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(plan, from, to);
        } else {
            workouts = plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan);
        }
        return workouts.stream().map(PlannedWorkoutResponse::from).toList();
    }

    @Transactional
    public void archive(User user, UUID planId) {
        TrainingPlan plan = findOwnedPlan(user, planId);
        if (plan.getStatus() != TrainingPlanStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only ACTIVE plans can be archived");
        }
        plan.setStatus(TrainingPlanStatus.ARCHIVED);
        trainingPlanRepository.save(plan);
    }

    private TrainingPlan findOwnedPlan(User user, UUID planId) {
        return trainingPlanRepository.findByIdAndUser(planId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Training plan not found"));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=TrainingPlanServiceTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/plan/TrainingPlanService.java
git commit -m "feat(plan): implement TrainingPlanService with generation and CRUD"
```

---

### Task 10: TrainingPlanController — Tests

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/plan/TrainingPlanControllerTest.java`

- [ ] **Step 1: Write test class with setup**

```java
package com.runplanner.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.config.SecurityConfig;
import com.runplanner.plan.dto.CreatePlanRequest;
import com.runplanner.plan.dto.PlannedWorkoutResponse;
import com.runplanner.plan.dto.TrainingPlanResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainingPlanController.class)
@Import(SecurityConfig.class)
class TrainingPlanControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean TrainingPlanService trainingPlanService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").build();
    }

    private TrainingPlanResponse samplePlanResponse() {
        return new TrainingPlanResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 1),
                "ACTIVE", 1, Instant.now(), List.of());
    }
}
```

- [ ] **Step 2: Write POST /plans tests**

Add to test class:

```java
@Test
void createPlan_returns201() throws Exception {
    User u = testUser();
    UUID raceId = UUID.randomUUID();
    when(trainingPlanService.generate(any(), eq(raceId))).thenReturn(samplePlanResponse());

    mockMvc.perform(post("/api/v1/plans")
                    .with(user(u))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CreatePlanRequest(raceId))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
}

@Test
void createPlan_activePlanExists_returns409() throws Exception {
    User u = testUser();
    UUID raceId = UUID.randomUUID();
    when(trainingPlanService.generate(any(), eq(raceId)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

    mockMvc.perform(post("/api/v1/plans")
                    .with(user(u))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CreatePlanRequest(raceId))))
            .andExpect(status().isConflict());
}

@Test
void createPlan_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 3: Write GET endpoint tests**

Add to test class:

```java
@Test
void listPlans_returnsAll() throws Exception {
    User u = testUser();
    when(trainingPlanService.findAll(any())).thenReturn(List.of(samplePlanResponse()));

    mockMvc.perform(get("/api/v1/plans").with(user(u)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
}

@Test
void getActivePlan_returnsPlan() throws Exception {
    User u = testUser();
    when(trainingPlanService.findActive(any())).thenReturn(samplePlanResponse());

    mockMvc.perform(get("/api/v1/plans/active").with(user(u)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
}

@Test
void getActivePlan_noPlan_returns404() throws Exception {
    User u = testUser();
    when(trainingPlanService.findActive(any()))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(get("/api/v1/plans/active").with(user(u)))
            .andExpect(status().isNotFound());
}

@Test
void getPlanById_returnsPlan() throws Exception {
    User u = testUser();
    TrainingPlanResponse response = samplePlanResponse();
    when(trainingPlanService.findById(any(), eq(response.id()))).thenReturn(response);

    mockMvc.perform(get("/api/v1/plans/" + response.id()).with(user(u)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revision").value(1));
}

@Test
void getPlannedWorkouts_returnsWorkouts() throws Exception {
    User u = testUser();
    UUID planId = UUID.randomUUID();
    PlannedWorkoutResponse workout = new PlannedWorkoutResponse(
            UUID.randomUUID(), 1, 2, LocalDate.of(2026, 1, 6),
            "EASY", 8000.0, 5.0, 6.0, null, null, 1);
    when(trainingPlanService.findPlannedWorkouts(any(), eq(planId), isNull(), isNull()))
            .thenReturn(List.of(workout));

    mockMvc.perform(get("/api/v1/plans/" + planId + "/workouts").with(user(u)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workoutType").value("EASY"));
}

@Test
void getPlannedWorkouts_withDateFilters() throws Exception {
    User u = testUser();
    UUID planId = UUID.randomUUID();
    when(trainingPlanService.findPlannedWorkouts(any(), eq(planId),
            eq(LocalDate.of(2026, 1, 5)), eq(LocalDate.of(2026, 1, 11))))
            .thenReturn(List.of());

    mockMvc.perform(get("/api/v1/plans/" + planId + "/workouts")
                    .param("from", "2026-01-05")
                    .param("to", "2026-01-11")
                    .with(user(u)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
}
```

- [ ] **Step 4: Write DELETE endpoint tests**

Add to test class:

```java
@Test
void archivePlan_returns204() throws Exception {
    User u = testUser();
    UUID planId = UUID.randomUUID();
    doNothing().when(trainingPlanService).archive(any(), eq(planId));

    mockMvc.perform(delete("/api/v1/plans/" + planId).with(user(u)))
            .andExpect(status().isNoContent());
}

@Test
void archivePlan_nonActive_returns400() throws Exception {
    User u = testUser();
    UUID planId = UUID.randomUUID();
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
            .when(trainingPlanService).archive(any(), eq(planId));

    mockMvc.perform(delete("/api/v1/plans/" + planId).with(user(u)))
            .andExpect(status().isBadRequest());
}

@Test
void allEndpoints_unauthenticated_return401() throws Exception {
    UUID id = UUID.randomUUID();
    mockMvc.perform(get("/api/v1/plans")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/plans/active")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/plans/" + id)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/plans/" + id + "/workouts")).andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/api/v1/plans/" + id)).andExpect(status().isUnauthorized());
}
```

- [ ] **Step 5: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=TrainingPlanControllerTest test`
Expected: COMPILATION FAILURE (TrainingPlanController does not exist yet)

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/runplanner/plan/TrainingPlanControllerTest.java
git commit -m "test(plan): add TrainingPlanController integration tests"
```

---

### Task 11: TrainingPlanController — Implementation

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/plan/TrainingPlanController.java`

- [ ] **Step 1: Implement TrainingPlanController**

```java
package com.runplanner.plan;

import com.runplanner.plan.dto.CreatePlanRequest;
import com.runplanner.plan.dto.PlannedWorkoutResponse;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingPlanResponse create(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody CreatePlanRequest request) {
        return trainingPlanService.generate(user, request.goalRaceId());
    }

    @GetMapping
    public List<TrainingPlanResponse> list(@AuthenticationPrincipal User user) {
        return trainingPlanService.findAll(user);
    }

    @GetMapping("/active")
    public TrainingPlanResponse getActive(@AuthenticationPrincipal User user) {
        return trainingPlanService.findActive(user);
    }

    @GetMapping("/{id}")
    public TrainingPlanResponse getById(@AuthenticationPrincipal User user,
                                        @PathVariable UUID id) {
        return trainingPlanService.findById(user, id);
    }

    @GetMapping("/{id}/workouts")
    public List<PlannedWorkoutResponse> getWorkouts(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return trainingPlanService.findPlannedWorkouts(user, id, from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal User user,
                        @PathVariable UUID id) {
        trainingPlanService.archive(user, id);
    }
}
```

- [ ] **Step 2: Run controller tests**

Run: `cd run-planner-backend && mvn -Dtest=TrainingPlanControllerTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/plan/TrainingPlanController.java
git commit -m "feat(plan): add TrainingPlanController with REST endpoints"
```

---

### Task 12: Full Test Suite Verification

- [ ] **Step 1: Run all tests**

Run: `cd run-planner-backend && mvn test`
Expected: All tests PASS (including all existing auth, goalrace, vdot, workout, and new plan tests)

- [ ] **Step 2: Verify build**

Run: `cd run-planner-backend && mvn clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 3: Final commit if any issues found**

If the full build reveals any issues, fix and commit:

```bash
git add -A
git commit -m "chore: fix issues from full build verification"
```
