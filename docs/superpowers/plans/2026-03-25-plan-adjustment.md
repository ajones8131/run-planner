# Plan Adjustment Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a plan adjustment engine that evaluates recent workout compliance and VDOT changes, detects both under- and over-performance, and applies major (regenerate) or minor (pace nudge) adjustments to the active training plan.

**Architecture:** `AdjustmentEvaluator` is pure decision logic returning an `AdjustmentDecision`. `PlanAdjuster` applies decisions by mutating the plan. `PlanAdjustmentEngine` orchestrates both, assembling data from repositories. Wired into `HealthSyncService` after matching/VDOT steps. Flyway V10 adds `lastAdjustmentVdot` to `training_plans`.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL, Flyway, JUnit 5, Mockito, AssertJ, Lombok

**Spec:** `docs/superpowers/specs/2026-03-25-plan-adjustment-design.md`

---

### Task 1: Migration, Value Types, Constants, and Entity Update

**Files:**
- Create: `src/main/resources/db/migration/V10__add_last_adjustment_vdot.sql`
- Create: `src/main/java/com/runplanner/adjustment/AdjustmentType.java`
- Create: `src/main/java/com/runplanner/adjustment/AdjustmentDecision.java`
- Create: `src/main/java/com/runplanner/adjustment/MatchedWorkoutContext.java`
- Create: `src/main/java/com/runplanner/adjustment/AdjustmentConstants.java`
- Modify: `src/main/java/com/runplanner/plan/TrainingPlan.java`
- Modify: `src/main/java/com/runplanner/plan/TrainingPlanService.java`

- [ ] **Step 1: Create V10 migration**

```sql
ALTER TABLE training_plans ADD COLUMN last_adjustment_vdot DOUBLE PRECISION;
```

- [ ] **Step 2: Create AdjustmentType enum**

```java
package com.runplanner.adjustment;

public enum AdjustmentType {
    NONE, MINOR, MAJOR
}
```

- [ ] **Step 3: Create AdjustmentDecision record**

```java
package com.runplanner.adjustment;

public record AdjustmentDecision(AdjustmentType type, String reason) {

    public static AdjustmentDecision none() {
        return new AdjustmentDecision(AdjustmentType.NONE, null);
    }
}
```

- [ ] **Step 4: Create MatchedWorkoutContext record**

```java
package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.plan.PlannedWorkout;
import com.runplanner.workout.Workout;

public record MatchedWorkoutContext(
        WorkoutMatch match,
        PlannedWorkout planned,
        Workout actual
) {}
```

- [ ] **Step 5: Create AdjustmentConstants**

```java
package com.runplanner.adjustment;

public final class AdjustmentConstants {

    private AdjustmentConstants() {}

    // Major triggers
    public static final double MAJOR_LOW_COMPLIANCE_THRESHOLD = 0.6;
    public static final int MAJOR_CONSECUTIVE_COUNT = 2;
    public static final double VDOT_CHANGE_THRESHOLD = 2.0;
    public static final double OVER_PERFORMANCE_PACE_FACTOR = 0.90;

    // Minor triggers
    public static final double MINOR_COMPLIANCE_LOW = 0.6;
    public static final double MINOR_COMPLIANCE_HIGH = 0.75;
    public static final int MINOR_WINDOW_SIZE = 5;
    public static final int MINOR_TRIGGER_COUNT = 3;

    // Time windows
    public static final int MISSED_LONG_RUN_WINDOW_DAYS = 7;
    public static final int RECENT_MATCH_WINDOW_DAYS = 14;
}
```

- [ ] **Step 6: Add lastAdjustmentVdot to TrainingPlan entity**

Add this field to `TrainingPlan.java` after the `createdAt` field:

```java
    private Double lastAdjustmentVdot;
```

- [ ] **Step 7: Set lastAdjustmentVdot in TrainingPlanService.generate()**

In `TrainingPlanService.java`, after the plan is built but before save, add `lastAdjustmentVdot` to the builder. Change the builder chain from:

```java
        TrainingPlan plan = trainingPlanRepository.save(TrainingPlan.builder()
                .user(user)
                .goalRace(race)
                .startDate(startDate)
                .endDate(race.getRaceDate())
                .build());
```

to:

```java
        TrainingPlan plan = trainingPlanRepository.save(TrainingPlan.builder()
                .user(user)
                .goalRace(race)
                .startDate(startDate)
                .endDate(race.getRaceDate())
                .lastAdjustmentVdot(vdot)
                .build());
```

- [ ] **Step 8: Add new repository methods**

Add to `WorkoutMatchRepository.java`:

```java
    List<WorkoutMatch> findByPlannedWorkoutIn(List<PlannedWorkout> plannedWorkouts);
```

Add to `PlannedWorkoutRepository.java`:

```java
    void deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(TrainingPlan trainingPlan, LocalDate date);

    List<PlannedWorkout> findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(
            TrainingPlan trainingPlan, LocalDate date);
```

- [ ] **Step 9: Verify compilation and existing tests pass**

Run: `cd run-planner-backend && mvn test -q`
Expected: All existing tests PASS

- [ ] **Step 10: Commit**

```bash
git add src/main/resources/db/migration/V10__add_last_adjustment_vdot.sql \
       src/main/java/com/runplanner/adjustment/ \
       src/main/java/com/runplanner/plan/TrainingPlan.java \
       src/main/java/com/runplanner/plan/TrainingPlanService.java \
       src/main/java/com/runplanner/match/WorkoutMatchRepository.java \
       src/main/java/com/runplanner/plan/PlannedWorkoutRepository.java
git commit -m "feat(adjustment): add V10 migration, value types, constants, entity update, and new repo methods"
```

---

### Task 2: AdjustmentEvaluator — Tests

**Files:**
- Create: `src/test/java/com/runplanner/adjustment/AdjustmentEvaluatorTest.java`

- [ ] **Step 1: Write test class with all tests**

```java
package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdjustmentEvaluatorTest {

    private final AdjustmentEvaluator evaluator = new AdjustmentEvaluator();

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("t@t.com").passwordHash("h").build();
    }

    private MatchedWorkoutContext matched(double complianceScore, double actualPaceMinPerKm,
                                           Double plannedPaceMin, Double plannedPaceMax,
                                           LocalDate scheduledDate) {
        User u = user();
        // Derive duration from pace and distance: pace * (distance/1000) * 60
        double distanceMeters = 10000.0;
        int durationSeconds = (int) (actualPaceMinPerKm * (distanceMeters / 1000.0) * 60);

        PlannedWorkout planned = PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .scheduledDate(scheduledDate)
                .workoutType(WorkoutType.EASY)
                .targetDistanceMeters(distanceMeters)
                .targetPaceMinPerKm(plannedPaceMin)
                .targetPaceMaxPerKm(plannedPaceMax)
                .originalScheduledDate(scheduledDate)
                .build();

        Workout actual = Workout.builder()
                .id(UUID.randomUUID())
                .user(u)
                .source("TEST")
                .startedAt(scheduledDate.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant())
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .build();

        WorkoutMatch match = WorkoutMatch.builder()
                .id(UUID.randomUUID())
                .plannedWorkout(planned)
                .workout(actual)
                .complianceScore(complianceScore)
                .build();

        return new MatchedWorkoutContext(match, planned, actual);
    }

    private PlannedWorkout unmatchedLong(LocalDate date) {
        return PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .scheduledDate(date)
                .workoutType(WorkoutType.LONG)
                .targetDistanceMeters(15000.0)
                .originalScheduledDate(date)
                .build();
    }

    // --- Empty / NONE ---

    @Test
    void evaluate_noMatches_returnsNone() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }

    // --- Major: consecutive under-performance ---

    @Test
    void evaluate_twoConsecutiveLowCompliance_returnsMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        var matches = List.of(
                matched(0.5, 5.25, 5.0, 5.5, d1),
                matched(0.4, 5.25, 5.0, 5.5, d2));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_oneHighOneLow_notConsecutive_returnsNone() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        LocalDate d3 = LocalDate.of(2026, 3, 25);
        var matches = List.of(
                matched(0.5, 5.25, 5.0, 5.5, d1),
                matched(0.8, 5.25, 5.0, 5.5, d2),
                matched(0.5, 5.25, 5.0, 5.5, d3));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    // --- Major: missed long run ---

    @Test
    void evaluate_missedLongRunInPast7Days_returnsMajor() {
        LocalDate recent = LocalDate.of(2026, 3, 22);
        var unmatched = List.of(unmatchedLong(recent));

        AdjustmentDecision decision = evaluator.evaluate(List.of(), unmatched, 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_missedLongRun8DaysAgo_returnsNone() {
        // This is handled by the engine's date filtering, not the evaluator
        // But if passed in, it should still trigger since evaluator trusts the input
        LocalDate old = LocalDate.of(2026, 3, 15);
        var unmatched = List.of(unmatchedLong(old));

        AdjustmentDecision decision = evaluator.evaluate(List.of(), unmatched, 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    // --- Major: VDOT change ---

    @Test
    void evaluate_vdotIncreasedMoreThan2_returnsMajor() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 53.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_vdotDecreasedMoreThan2_returnsMajor() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 47.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_vdotChangedExactly2_returnsNone() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 52.0, 50.0);
        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    // --- Major: consecutive over-performance ---

    @Test
    void evaluate_twoConsecutiveOverPerformance_returnsMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        // Midpoint = 5.25, 10% faster = 4.725. Actual pace 4.5 < 4.725 → over-performance
        var matches = List.of(
                matched(0.8, 4.5, 5.0, 5.5, d1),
                matched(0.8, 4.5, 5.0, 5.5, d2));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_overPerformanceNotConsecutive_noMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        LocalDate d3 = LocalDate.of(2026, 3, 25);
        var matches = List.of(
                matched(0.8, 4.5, 5.0, 5.5, d1),
                matched(0.8, 5.25, 5.0, 5.5, d2),  // normal pace
                matched(0.8, 4.5, 5.0, 5.5, d3));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_overPerformanceNullPaces_skipped() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        // Null paces should be skipped, not counted as over-performance
        var matches = List.of(
                matched(0.8, 4.5, null, null, d1),
                matched(0.8, 4.5, null, null, d2));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    // --- Minor: under-performance drift ---

    @Test
    void evaluate_threeOfFiveMediumCompliance_returnsMinor() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        var matches = List.of(
                matched(0.65, 5.25, 5.0, 5.5, base),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.7, 5.25, 5.0, 5.5, base.plusDays(2)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.72, 5.25, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MINOR);
    }

    @Test
    void evaluate_twoOfFiveMediumCompliance_returnsNone() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        var matches = List.of(
                matched(0.65, 5.25, 5.0, 5.5, base),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.7, 5.25, 5.0, 5.5, base.plusDays(2)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }

    // --- Minor: over-performance drift ---

    @Test
    void evaluate_threeOfFiveFasterThanPaceMin_returnsMinor() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        // paceMin = 5.0, actual 4.8 < 5.0 → faster than fast end
        var matches = List.of(
                matched(0.9, 4.8, 5.0, 5.5, base),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.9, 4.8, 5.0, 5.5, base.plusDays(2)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.9, 4.8, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MINOR);
    }

    // --- Priority ---

    @Test
    void evaluate_majorTakesPrecedenceOverMinor() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        // Both major (consecutive low) and minor (3 of 5 medium) conditions met
        var matches = List.of(
                matched(0.65, 5.25, 5.0, 5.5, base),
                matched(0.65, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.5, 5.25, 5.0, 5.5, base.plusDays(2)),
                matched(0.5, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.65, 5.25, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_emptyMatchList_returnsNone() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=AdjustmentEvaluatorTest test`
Expected: COMPILATION FAILURE

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/runplanner/adjustment/AdjustmentEvaluatorTest.java
git commit -m "test(adjustment): add AdjustmentEvaluator unit tests for all trigger conditions"
```

---

### Task 3: AdjustmentEvaluator — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/adjustment/AdjustmentEvaluator.java`

- [ ] **Step 1: Implement AdjustmentEvaluator**

```java
package com.runplanner.adjustment;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.workout.Workout;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.runplanner.adjustment.AdjustmentConstants.*;

@Component
public class AdjustmentEvaluator {

    public AdjustmentDecision evaluate(List<MatchedWorkoutContext> recentMatches,
                                        List<PlannedWorkout> recentUnmatched,
                                        double currentVdot,
                                        double lastAdjustmentVdot) {

        // Major triggers
        if (hasConsecutiveLowCompliance(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "Consecutive workouts below compliance threshold");
        }

        if (hasMissedLongRun(recentUnmatched)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "Missed long run");
        }

        if (hasSignificantVdotChange(currentVdot, lastAdjustmentVdot)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "VDOT changed by more than " + VDOT_CHANGE_THRESHOLD + " points");
        }

        if (hasConsecutiveOverPerformance(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "Consecutive workouts significantly faster than target");
        }

        // Minor triggers
        if (hasUnderPerformanceDrift(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MINOR,
                    "Recent compliance trending below optimal range");
        }

        if (hasOverPerformanceDrift(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MINOR,
                    "Recent pace consistently faster than target range");
        }

        return AdjustmentDecision.none();
    }

    private boolean hasConsecutiveLowCompliance(List<MatchedWorkoutContext> matches) {
        int consecutive = 0;
        for (MatchedWorkoutContext ctx : matches) {
            if (ctx.match().getComplianceScore() < MAJOR_LOW_COMPLIANCE_THRESHOLD) {
                consecutive++;
                if (consecutive >= MAJOR_CONSECUTIVE_COUNT) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    private boolean hasMissedLongRun(List<PlannedWorkout> unmatched) {
        return unmatched.stream()
                .anyMatch(pw -> pw.getWorkoutType() == WorkoutType.LONG);
    }

    private boolean hasSignificantVdotChange(double currentVdot, double lastAdjustmentVdot) {
        return Math.abs(currentVdot - lastAdjustmentVdot) > VDOT_CHANGE_THRESHOLD;
    }

    private boolean hasConsecutiveOverPerformance(List<MatchedWorkoutContext> matches) {
        int consecutive = 0;
        for (MatchedWorkoutContext ctx : matches) {
            if (isOverPerforming(ctx)) {
                consecutive++;
                if (consecutive >= MAJOR_CONSECUTIVE_COUNT) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    private boolean isOverPerforming(MatchedWorkoutContext ctx) {
        PlannedWorkout planned = ctx.planned();
        if (planned.getTargetPaceMinPerKm() == null || planned.getTargetPaceMaxPerKm() == null) {
            return false;
        }
        double midpoint = (planned.getTargetPaceMinPerKm() + planned.getTargetPaceMaxPerKm()) / 2.0;
        double actualPace = calculateActualPace(ctx.actual());
        return actualPace < midpoint * OVER_PERFORMANCE_PACE_FACTOR;
    }

    private boolean hasUnderPerformanceDrift(List<MatchedWorkoutContext> matches) {
        List<MatchedWorkoutContext> window = lastN(matches, MINOR_WINDOW_SIZE);
        if (window.size() < MINOR_WINDOW_SIZE) return false;
        long count = window.stream()
                .filter(ctx -> {
                    double score = ctx.match().getComplianceScore();
                    return score >= MINOR_COMPLIANCE_LOW && score <= MINOR_COMPLIANCE_HIGH;
                })
                .count();
        return count >= MINOR_TRIGGER_COUNT;
    }

    private boolean hasOverPerformanceDrift(List<MatchedWorkoutContext> matches) {
        List<MatchedWorkoutContext> window = lastN(matches, MINOR_WINDOW_SIZE);
        if (window.size() < MINOR_WINDOW_SIZE) return false;
        long count = window.stream()
                .filter(ctx -> {
                    PlannedWorkout planned = ctx.planned();
                    if (planned.getTargetPaceMinPerKm() == null) return false;
                    double actualPace = calculateActualPace(ctx.actual());
                    return actualPace < planned.getTargetPaceMinPerKm();
                })
                .count();
        return count >= MINOR_TRIGGER_COUNT;
    }

    private double calculateActualPace(Workout workout) {
        return (workout.getDurationSeconds() / 60.0) / (workout.getDistanceMeters() / 1000.0);
    }

    private <T> List<T> lastN(List<T> list, int n) {
        if (list.size() <= n) return list;
        return list.subList(list.size() - n, list.size());
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=AdjustmentEvaluatorTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/adjustment/AdjustmentEvaluator.java
git commit -m "feat(adjustment): implement AdjustmentEvaluator with under/over-performance detection"
```

---

### Task 4: PlanAdjuster — Tests

**Files:**
- Create: `src/test/java/com/runplanner/adjustment/PlanAdjusterTest.java`

- [ ] **Step 1: Write test class with all tests**

```java
package com.runplanner.adjustment;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import com.runplanner.vdot.VdotHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAdjusterTest {

    @Mock private TrainingPlanGenerator trainingPlanGenerator;
    @Mock private TrainingPaceCalculator trainingPaceCalculator;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private Clock clock;

    @InjectMocks private PlanAdjuster adjuster;

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-03-25T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("t@t.com").passwordHash("h").build();
    }

    private TrainingPlan plan(User user) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .goalRace(GoalRace.builder().id(UUID.randomUUID()).distanceMeters(21097)
                        .raceDate(LocalDate.of(2026, 6, 1)).distanceLabel("HM").build())
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 1))
                .status(TrainingPlanStatus.ACTIVE)
                .revision(1)
                .build();
    }

    @Test
    void apply_major_deletesFutureAndRegenerates() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(trainingPlanGenerator.generate(eq(50.0), eq(21097), eq(p.getEndDate()), eq(LocalDate.of(2026, 3, 25))))
                .thenReturn(List.of(
                        PlannedWorkout.builder().weekNumber(1).dayOfWeek(1)
                                .scheduledDate(LocalDate.of(2026, 3, 25))
                                .workoutType(WorkoutType.REST).targetDistanceMeters(0)
                                .originalScheduledDate(LocalDate.of(2026, 3, 25)).build()));
        when(plannedWorkoutRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(trainingPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adjuster.apply(p, AdjustmentType.MAJOR, user);

        verify(plannedWorkoutRepository).deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(
                p, LocalDate.of(2026, 3, 25));
        verify(trainingPlanGenerator).generate(50.0, 21097, p.getEndDate(), LocalDate.of(2026, 3, 25));
        verify(plannedWorkoutRepository).saveAll(anyList());
        assertThat(p.getRevision()).isEqualTo(2);
    }

    @Test
    void apply_minor_updatesPacesOnFutureWorkouts() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(52.0));
        Map<TrainingZone, PaceRange> paces = Map.of(
                TrainingZone.E, new PaceRange(4.8, 5.8),
                TrainingZone.M, new PaceRange(4.3, 4.5),
                TrainingZone.T, new PaceRange(3.8, 4.1),
                TrainingZone.I, new PaceRange(3.3, 3.6),
                TrainingZone.R, new PaceRange(2.8, 3.1));
        when(trainingPaceCalculator.calculate(52.0)).thenReturn(paces);

        PlannedWorkout easyWorkout = PlannedWorkout.builder()
                .id(UUID.randomUUID()).trainingPlan(p)
                .scheduledDate(LocalDate.of(2026, 3, 26))
                .workoutType(WorkoutType.EASY)
                .targetDistanceMeters(10000).targetPaceMinPerKm(5.0).targetPaceMaxPerKm(6.0)
                .originalScheduledDate(LocalDate.of(2026, 3, 26)).build();
        PlannedWorkout restWorkout = PlannedWorkout.builder()
                .id(UUID.randomUUID()).trainingPlan(p)
                .scheduledDate(LocalDate.of(2026, 3, 27))
                .workoutType(WorkoutType.REST)
                .targetDistanceMeters(0)
                .originalScheduledDate(LocalDate.of(2026, 3, 27)).build();

        when(plannedWorkoutRepository
                .findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(
                        p, LocalDate.of(2026, 3, 25)))
                .thenReturn(List.of(easyWorkout, restWorkout));
        when(plannedWorkoutRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(trainingPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adjuster.apply(p, AdjustmentType.MINOR, user);

        // Easy workout paces updated to E zone (4.8-5.8)
        assertThat(easyWorkout.getTargetPaceMinPerKm()).isEqualTo(4.8);
        assertThat(easyWorkout.getTargetPaceMaxPerKm()).isEqualTo(5.8);
        // REST workout paces unchanged (null)
        assertThat(restWorkout.getTargetPaceMinPerKm()).isNull();
        assertThat(p.getRevision()).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=PlanAdjusterTest test`
Expected: COMPILATION FAILURE

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/runplanner/adjustment/PlanAdjusterTest.java
git commit -m "test(adjustment): add PlanAdjuster unit tests for major and minor adjustments"
```

---

### Task 5: PlanAdjuster — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/adjustment/PlanAdjuster.java`

- [ ] **Step 1: Implement PlanAdjuster**

```java
package com.runplanner.adjustment;

import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import com.runplanner.vdot.VdotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanAdjuster {

    private final TrainingPlanGenerator trainingPlanGenerator;
    private final TrainingPaceCalculator trainingPaceCalculator;
    private final VdotHistoryService vdotHistoryService;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final Clock clock;

    public void apply(TrainingPlan plan, AdjustmentType type, User user) {
        double vdot = vdotHistoryService.getEffectiveVdot(user).orElseThrow();
        LocalDate today = LocalDate.now(clock);

        if (type == AdjustmentType.MAJOR) {
            applyMajor(plan, vdot, today);
        } else if (type == AdjustmentType.MINOR) {
            applyMinor(plan, vdot, today);
        }

        plan.setRevision(plan.getRevision() + 1);
        trainingPlanRepository.save(plan);
    }

    private void applyMajor(TrainingPlan plan, double vdot, LocalDate today) {
        plannedWorkoutRepository.deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(plan, today);

        List<PlannedWorkout> newWorkouts = trainingPlanGenerator.generate(
                vdot, plan.getGoalRace().getDistanceMeters(), plan.getEndDate(), today);

        int newRevision = plan.getRevision() + 1;
        newWorkouts.forEach(w -> {
            w.setTrainingPlan(plan);
            w.setPlanRevision(newRevision);
        });

        plannedWorkoutRepository.saveAll(newWorkouts);
    }

    private void applyMinor(TrainingPlan plan, double vdot, LocalDate today) {
        Map<TrainingZone, PaceRange> paces = trainingPaceCalculator.calculate(vdot);

        List<PlannedWorkout> futureWorkouts = plannedWorkoutRepository
                .findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(plan, today);

        for (PlannedWorkout workout : futureWorkouts) {
            TrainingZone zone = workout.getWorkoutType().getTrainingZone();
            if (zone == null) continue;

            PaceRange pace = paces.get(zone);
            workout.setTargetPaceMinPerKm(pace.minPaceMinPerKm());
            workout.setTargetPaceMaxPerKm(pace.maxPaceMinPerKm());
        }

        plannedWorkoutRepository.saveAll(futureWorkouts);
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=PlanAdjusterTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/adjustment/PlanAdjuster.java
git commit -m "feat(adjustment): implement PlanAdjuster for major and minor plan modifications"
```

---

### Task 6: PlanAdjustmentEngine — Tests

**Files:**
- Create: `src/test/java/com/runplanner/adjustment/PlanAdjustmentEngineTest.java`

- [ ] **Step 1: Write test class with all tests**

```java
package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.match.WorkoutMatchRepository;
import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAdjustmentEngineTest {

    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private WorkoutMatchRepository workoutMatchRepository;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private AdjustmentEvaluator adjustmentEvaluator;
    @Mock private PlanAdjuster planAdjuster;
    @Mock private Clock clock;

    @InjectMocks private PlanAdjustmentEngine engine;

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-03-25T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("t@t.com").passwordHash("h").build();
    }

    private TrainingPlan plan(User user, Double lastAdjVdot) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID()).user(user)
                .status(TrainingPlanStatus.ACTIVE)
                .lastAdjustmentVdot(lastAdjVdot)
                .build();
    }

    @Test
    void evaluate_noActivePlan_returnsNone() {
        User user = user();
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
        verify(planAdjuster, never()).apply(any(), any(), any());
    }

    @Test
    void evaluate_noVdot_returnsNone() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, 50.0);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
        verify(planAdjuster, never()).apply(any(), any(), any());
    }

    @Test
    void evaluate_evaluatorReturnsMajor_adjusterCalled() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, 50.0);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(workoutMatchRepository.findByPlannedWorkoutIn(anyList())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(53.0));
        when(adjustmentEvaluator.evaluate(anyList(), anyList(), eq(53.0), eq(50.0)))
                .thenReturn(new AdjustmentDecision(AdjustmentType.MAJOR, "VDOT changed"));

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
        verify(planAdjuster).apply(p, AdjustmentType.MAJOR, user);
        assertThat(p.getLastAdjustmentVdot()).isEqualTo(53.0);
    }

    @Test
    void evaluate_evaluatorReturnsNone_adjusterNotCalled() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, 50.0);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(workoutMatchRepository.findByPlannedWorkoutIn(anyList())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(adjustmentEvaluator.evaluate(anyList(), anyList(), eq(50.0), eq(50.0)))
                .thenReturn(AdjustmentDecision.none());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
        verify(planAdjuster, never()).apply(any(), any(), any());
    }

    @Test
    void evaluate_nullLastAdjustmentVdot_usesCurrentVdot() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, null); // never adjusted
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(workoutMatchRepository.findByPlannedWorkoutIn(anyList())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        // lastAdjustmentVdot is null → uses currentVdot (50.0), so no VDOT change
        when(adjustmentEvaluator.evaluate(anyList(), anyList(), eq(50.0), eq(50.0)))
                .thenReturn(AdjustmentDecision.none());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run-planner-backend && mvn -Dtest=PlanAdjustmentEngineTest test`
Expected: COMPILATION FAILURE

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/runplanner/adjustment/PlanAdjustmentEngineTest.java
git commit -m "test(adjustment): add PlanAdjustmentEngine unit tests"
```

---

### Task 7: PlanAdjustmentEngine — Implementation

**Files:**
- Create: `src/main/java/com/runplanner/adjustment/PlanAdjustmentEngine.java`

- [ ] **Step 1: Implement PlanAdjustmentEngine**

```java
package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.match.WorkoutMatchRepository;
import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.runplanner.adjustment.AdjustmentConstants.*;

@Service
@RequiredArgsConstructor
public class PlanAdjustmentEngine {

    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final WorkoutMatchRepository workoutMatchRepository;
    private final VdotHistoryService vdotHistoryService;
    private final AdjustmentEvaluator adjustmentEvaluator;
    private final PlanAdjuster planAdjuster;
    private final Clock clock;

    @Transactional
    public AdjustmentDecision evaluate(User user) {
        var activePlan = trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE);
        if (activePlan.isEmpty()) {
            return AdjustmentDecision.none();
        }

        TrainingPlan plan = activePlan.get();
        LocalDate today = LocalDate.now(clock);

        // Get recent planned workouts (past 14 days)
        LocalDate windowStart = today.minusDays(RECENT_MATCH_WINDOW_DAYS);
        List<PlannedWorkout> recentPlanned = plannedWorkoutRepository
                .findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                        plan, windowStart, today.minusDays(1));

        // Build matched workout contexts
        List<WorkoutMatch> matches = recentPlanned.isEmpty()
                ? List.of()
                : workoutMatchRepository.findByPlannedWorkoutIn(recentPlanned);

        Map<PlannedWorkout, WorkoutMatch> matchMap = matches.stream()
                .collect(Collectors.toMap(WorkoutMatch::getPlannedWorkout, Function.identity()));

        List<MatchedWorkoutContext> matchedContexts = new ArrayList<>();
        List<PlannedWorkout> unmatchedLong = new ArrayList<>();

        for (PlannedWorkout pw : recentPlanned) {
            WorkoutMatch match = matchMap.get(pw);
            if (match != null) {
                matchedContexts.add(new MatchedWorkoutContext(match, pw, match.getWorkout()));
            } else if (pw.getWorkoutType() == WorkoutType.LONG
                    && pw.getScheduledDate().isAfter(today.minusDays(MISSED_LONG_RUN_WINDOW_DAYS + 1))) {
                unmatchedLong.add(pw);
            }
        }

        // Get VDOT
        var effectiveVdot = vdotHistoryService.getEffectiveVdot(user);
        if (effectiveVdot.isEmpty()) {
            return AdjustmentDecision.none();
        }

        double currentVdot = effectiveVdot.get();
        double lastAdjVdot = plan.getLastAdjustmentVdot() != null
                ? plan.getLastAdjustmentVdot()
                : currentVdot;

        // Evaluate
        AdjustmentDecision decision = adjustmentEvaluator.evaluate(
                matchedContexts, unmatchedLong, currentVdot, lastAdjVdot);

        // Apply if needed
        if (decision.type() != AdjustmentType.NONE) {
            planAdjuster.apply(plan, decision.type(), user);
            plan.setLastAdjustmentVdot(currentVdot);
            trainingPlanRepository.save(plan);
        }

        return decision;
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd run-planner-backend && mvn -Dtest=PlanAdjustmentEngineTest test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/runplanner/adjustment/PlanAdjustmentEngine.java
git commit -m "feat(adjustment): implement PlanAdjustmentEngine orchestrating evaluation and adjustment"
```

---

### Task 8: Health Sync Wiring

**Files:**
- Modify: `src/main/java/com/runplanner/health/HealthSyncService.java`
- Modify: `src/main/java/com/runplanner/health/dto/HealthSyncResponse.java`
- Modify: `src/test/java/com/runplanner/health/HealthSyncServiceTest.java`

- [ ] **Step 1: Update HealthSyncResponse to include adjustmentApplied**

Replace the existing `HealthSyncResponse.java` content with:

```java
package com.runplanner.health.dto;

public record HealthSyncResponse(
        int workoutsSaved,
        int workoutsSkipped,
        int workoutsMatched,
        int snapshotsSaved,
        boolean vdotUpdated,
        String adjustmentApplied
) {}
```

- [ ] **Step 2: Wire PlanAdjustmentEngine into HealthSyncService**

Add `PlanAdjustmentEngine` as a dependency. Add this import:

```java
import com.runplanner.adjustment.AdjustmentDecision;
import com.runplanner.adjustment.PlanAdjustmentEngine;
```

Add the field:

```java
    private final PlanAdjustmentEngine planAdjustmentEngine;
```

After step 4 (VDOT from VO2max) and before step 5 (lastSyncedAt), add:

```java
        // 5. Run adjustment engine
        AdjustmentDecision adjustment = planAdjustmentEngine.evaluate(user);
```

Update the return statement to include `adjustment.type().name()`:

```java
        return new HealthSyncResponse(
                workoutsSaved, workoutsSkipped, workoutsMatched, snapshotsSaved, vdotUpdated,
                adjustment.type().name());
```

- [ ] **Step 3: Update existing HealthSyncServiceTest**

Add `@Mock private PlanAdjustmentEngine planAdjustmentEngine;` to the test class.

In every existing test that calls `service.sync()`, add this stub (before the sync call):

```java
when(planAdjustmentEngine.evaluate(any())).thenReturn(AdjustmentDecision.none());
```

Note: `AdjustmentDecision.none()` requires importing `com.runplanner.adjustment.AdjustmentDecision`.

Also update any assertions on the response that check field counts — the response now has 6 fields.

Add a new test:

```java
@Test
void sync_triggersAdjustmentEngine() {
    User user = user();
    when(planAdjustmentEngine.evaluate(user))
            .thenReturn(new AdjustmentDecision(
                    com.runplanner.adjustment.AdjustmentType.MINOR, "pace drift"));

    HealthSyncRequest request = new HealthSyncRequest(null, null);
    HealthSyncResponse response = service.sync(user, request);

    assertThat(response.adjustmentApplied()).isEqualTo("MINOR");
    verify(planAdjustmentEngine).evaluate(user);
}
```

- [ ] **Step 4: Update HealthSyncControllerTest**

Update the mocked responses to include the 6th field. Change:

```java
new HealthSyncResponse(2, 1, 1, 1, true)
```

to:

```java
new HealthSyncResponse(2, 1, 1, 1, true, "NONE")
```

And:

```java
new HealthSyncResponse(0, 0, 0, 0, false)
```

to:

```java
new HealthSyncResponse(0, 0, 0, 0, false, "NONE")
```

- [ ] **Step 5: Run all health and adjustment tests**

Run: `cd run-planner-backend && mvn -Dtest='HealthSyncServiceTest,HealthSyncControllerTest,AdjustmentEvaluatorTest,PlanAdjusterTest,PlanAdjustmentEngineTest' test`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/runplanner/health/HealthSyncService.java \
       src/main/java/com/runplanner/health/dto/HealthSyncResponse.java \
       src/test/java/com/runplanner/health/HealthSyncServiceTest.java \
       src/test/java/com/runplanner/health/HealthSyncControllerTest.java
git commit -m "feat(adjustment): wire PlanAdjustmentEngine into HealthSyncService"
```

---

### Task 9: Full Test Suite Verification

- [ ] **Step 1: Run all tests**

Run: `cd run-planner-backend && mvn test`
Expected: All tests PASS

- [ ] **Step 2: Verify build**

Run: `cd run-planner-backend && mvn clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 3: Final commit if any issues found**

```bash
git add -A
git commit -m "chore: fix issues from full build verification"
```
