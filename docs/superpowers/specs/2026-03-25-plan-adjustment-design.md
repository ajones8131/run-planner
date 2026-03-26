# Plan Adjustment Engine Design

## Scope

Implements the plan adjustment engine that evaluates recent workout compliance and VDOT changes, then modifies the active training plan when performance deviates from targets. Detects both under-performance and over-performance. Two adjustment tiers: major (regenerate remaining plan) and minor (pace nudge). Wired into `HealthSyncService` to run automatically during sync. No Claude AI — deterministic Daniels math only.

## Database Migration (V10)

```sql
ALTER TABLE training_plans ADD COLUMN last_adjustment_vdot DOUBLE PRECISION;
```

`lastAdjustmentVdot` tracks the effective VDOT at the time of the last adjustment (or plan generation). Set when the plan is generated, updated after each adjustment. Prevents re-triggering VDOT-based adjustments on the same change.

## AdjustmentDecision

Record in `com.runplanner.adjustment`:

```java
public record AdjustmentDecision(AdjustmentType type, String reason) {}
```

## AdjustmentType

Enum: `NONE`, `MINOR`, `MAJOR`

## MatchedWorkoutContext

Record bundling the data the evaluator needs:

```java
public record MatchedWorkoutContext(
    WorkoutMatch match,
    PlannedWorkout planned,
    Workout actual
) {}
```

## AdjustmentEvaluator

Pure logic class with `@Component` in `com.runplanner.adjustment`. No side effects — takes data in, returns a decision.

**Method:** `AdjustmentDecision evaluate(List<MatchedWorkoutContext> recentMatches, List<PlannedWorkout> recentUnmatched, double currentVdot, double lastAdjustmentVdot)`

### Major triggers (any one triggers)

1. **Consecutive under-performance:** 2+ consecutive matched planned workouts (ordered by scheduled date) with `compliance_score < 0.6`
2. **Missed long run:** A planned workout with `WorkoutType.LONG` in `recentUnmatched` that was scheduled within the past 7 days
3. **VDOT change:** `|currentVdot - lastAdjustmentVdot| > 2.0` (fitness change in either direction)
4. **Consecutive over-performance:** 2+ consecutive matched planned workouts where actual pace is > 10% faster than target pace midpoint. Actual pace = `workout.durationSeconds / 60.0 / (workout.distanceMeters / 1000.0)`. Target midpoint = `(planned.targetPaceMinPerKm + planned.targetPaceMaxPerKm) / 2.0`. Over-performance = actual pace < midpoint * 0.90.

### Minor triggers (only if no major)

5. **Under-performance drift:** 3 of the last 5 matched planned workouts have `compliance_score` between 0.6 and 0.75
6. **Over-performance drift:** 3 of the last 5 matched planned workouts have actual pace faster than `planned.targetPaceMinPerKm` (the fast end of the range)

### Priority

Major takes precedence. Evaluation stops at the first major trigger found. Minor is only checked if no major triggered.

### Evaluation order

1. Check consecutive low compliance → MAJOR
2. Check missed long run → MAJOR
3. Check VDOT change → MAJOR
4. Check consecutive over-performance → MAJOR
5. Check minor under-performance drift → MINOR
6. Check minor over-performance drift → MINOR
7. Return NONE

## AdjustmentConstants

```
MAJOR_LOW_COMPLIANCE_THRESHOLD = 0.6
MAJOR_CONSECUTIVE_COUNT = 2
VDOT_CHANGE_THRESHOLD = 2.0
OVER_PERFORMANCE_PACE_FACTOR = 0.90
MINOR_COMPLIANCE_LOW = 0.6
MINOR_COMPLIANCE_HIGH = 0.75
MINOR_WINDOW_SIZE = 5
MINOR_TRIGGER_COUNT = 3
MISSED_LONG_RUN_WINDOW_DAYS = 7
```

## PlanAdjuster

Service in `com.runplanner.adjustment` that applies the decision to the plan.

**Method:** `void apply(TrainingPlan plan, AdjustmentType type, User user)`

### Major adjustment

1. Get current effective VDOT via `VdotHistoryService.getEffectiveVdot()`
2. Delete all future planned workouts (scheduled date >= today) via `PlannedWorkoutRepository`
3. Call `TrainingPlanGenerator.generate()` with current VDOT, goal race distance, race date, and today as start date
4. Associate new workouts with the plan, set `planRevision` to `plan.revision + 1`
5. Increment `plan.revision` and save

### Minor adjustment

1. Get current effective VDOT
2. Recalculate pace ranges using `TrainingPaceCalculator.calculate(vdot)`
3. Update `targetPaceMinPerKm` and `targetPaceMaxPerKm` on all future planned workouts (scheduled date >= today), mapping `WorkoutType.getTrainingZone()` for the pace lookup. REST workouts are skipped (no paces).
4. Save updated workouts
5. Increment `plan.revision` and save

### Dependencies

`TrainingPlanGenerator`, `TrainingPaceCalculator`, `VdotHistoryService`, `PlannedWorkoutRepository`, `TrainingPlanRepository`, `Clock`

## PlanAdjustmentEngine

Orchestrator in `com.runplanner.adjustment`.

**Method:** `AdjustmentDecision evaluate(User user)`

### Flow

1. Find active plan via `TrainingPlanRepository.findByUserAndStatus(user, ACTIVE)`. If none, return `NONE`.
2. Get recent matched workouts: planned workouts from the last 2 weeks with their `WorkoutMatch` and `Workout`, ordered by scheduled date. Assembled as `List<MatchedWorkoutContext>`.
3. Get recent unmatched LONG planned workouts from the last 7 days (scheduled in past 7 days, `WorkoutType.LONG`, no match exists).
4. Get current effective VDOT via `VdotHistoryService.getEffectiveVdot()`. If no VDOT, return `NONE`.
5. Get `plan.lastAdjustmentVdot` (or use current VDOT if null — plan was never adjusted).
6. Call `AdjustmentEvaluator.evaluate()` with the assembled data.
7. If decision is not NONE, call `PlanAdjuster.apply()` and update `plan.lastAdjustmentVdot` to current VDOT.
8. Return the decision.

### Dependencies

`TrainingPlanRepository`, `PlannedWorkoutRepository`, `WorkoutMatchRepository`, `VdotHistoryService`, `AdjustmentEvaluator`, `PlanAdjuster`, `Clock`

### New repository methods needed

- `WorkoutMatchRepository.findByPlannedWorkoutIn(List<PlannedWorkout>)` — batch lookup matches for recent planned workouts
- `PlannedWorkoutRepository.deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(TrainingPlan, LocalDate)` — delete future workouts for major adjustment
- `PlannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(TrainingPlan, LocalDate)` — future workouts for minor adjustment

## Entity Changes

### TrainingPlan

Add field: `lastAdjustmentVdot` (Double, nullable). Set in `TrainingPlanService.generate()` to the effective VDOT used for generation. Updated by `PlanAdjustmentEngine` after each adjustment.

## Health Sync Wiring

Add `PlanAdjustmentEngine.evaluate(user)` call in `HealthSyncService.sync()` after step 4 (VDOT from VO2max), before updating `lastSyncedAt`.

Add `adjustmentApplied` field (String: "NONE", "MINOR", "MAJOR") to `HealthSyncResponse`.

## Testing Strategy

### AdjustmentEvaluator (unit tests)

Pure logic, the bulk of testing:
- No matches at all → NONE
- 2 consecutive low compliance (< 0.6) → MAJOR
- 1 low compliance (not consecutive) → NONE
- Missed long run in past 7 days → MAJOR
- Missed long run 8+ days ago → NONE
- VDOT increased > 2 points → MAJOR
- VDOT decreased > 2 points → MAJOR
- VDOT changed exactly 2 points → NONE
- 2 consecutive over-performance (pace > 10% faster) → MAJOR
- Over-performance not consecutive → NONE
- 3 of 5 compliance between 0.6-0.75 → MINOR
- 2 of 5 compliance between 0.6-0.75 → NONE
- 3 of 5 faster than pace min → MINOR
- Major takes precedence over minor when both triggered
- Empty match list → NONE

### PlanAdjuster (unit tests, mocked deps)

- Major: deletes future workouts, generates new ones, increments revision
- Minor: updates paces on future workouts, increments revision
- Only future workouts affected (past preserved)

### PlanAdjustmentEngine (unit tests, mocked deps)

- Active plan, evaluator returns MAJOR, adjuster called
- No active plan → NONE, adjuster not called
- Evaluator returns NONE → adjuster not called
- Updates lastAdjustmentVdot after adjustment

### HealthSyncService update

- Existing tests still pass
- New test: sync triggers adjustment engine after matching/VDOT

## Package Structure

```
com.runplanner.adjustment/
    AdjustmentType.java           (enum)
    AdjustmentDecision.java       (record)
    MatchedWorkoutContext.java    (record)
    AdjustmentConstants.java     (thresholds)
    AdjustmentEvaluator.java     (pure evaluation logic, @Component)
    PlanAdjuster.java            (applies adjustments, @Service)
    PlanAdjustmentEngine.java    (orchestrator, @Service)
```
