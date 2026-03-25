# Workout Matching Feature Design

## Scope

Implements `workout_matches` entity, a matching algorithm that pairs completed workouts to planned workouts, and a compliance scoring formula. Purely internal — no REST endpoints, not auto-triggered. The future health sync endpoint or adjustment engine will call the matcher. No plan adjustment logic (separate feature).

## Database Migration (V8)

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

- Unique indexes on both FKs independently — a planned workout matches at most one real workout, and vice versa
- `compliance_score` is 0.0-1.0
- CASCADE deletes on both FKs

## Entity

`WorkoutMatch` in `com.runplanner.match` package. Bridges the `workout` and `plan` domains, so it gets its own package.

Fields: `id` (UUID, generated), `plannedWorkout` (ManyToOne lazy FK), `workout` (ManyToOne lazy FK), `complianceScore` (double, not null).

## Repository

`WorkoutMatchRepository extends JpaRepository<WorkoutMatch, UUID>`:

- `Optional<WorkoutMatch> findByPlannedWorkout(PlannedWorkout)` — check if a planned workout is already matched
- `Optional<WorkoutMatch> findByWorkout(Workout)` — check if a workout is already matched
- `boolean existsByPlannedWorkout(PlannedWorkout)` — quick check for matching logic
- `boolean existsByWorkout(Workout)` — skip already-matched workouts

## ComplianceScorer

Pure math class with `@Component` in `com.runplanner.match`.

**Method:** `double score(Workout workout, PlannedWorkout plannedWorkout)`

Returns 0.0 for REST planned workouts (should not be matched).

### Three-factor weighted average

| Factor | Weight | Calculation |
|--------|--------|-------------|
| Distance | 40% | `min(actual_distance / target_distance, 1.0)` |
| Pace | 40% | `1.0 - clamp(\|actual_pace - target_pace_mid\| / target_pace_range, 0, 1)` |
| HR zone | 20% | `1.0` if avg HR within target zone, `0.5` if within one adjacent zone, `0.0` otherwise |

### Pace calculation details

- `actual_pace` = `workout.durationSeconds / 60.0 / (workout.distanceMeters / 1000.0)` (min/km)
- `target_pace_mid` = average of `plannedWorkout.targetPaceMinPerKm` and `targetPaceMaxPerKm`
- `target_pace_range` = `targetPaceMaxPerKm - targetPaceMinPerKm`
- If pace range is zero (min == max), exact match = 1.0, any deviation = 0.0
- If planned paces are null (defensive), pace factor defaults to 1.0

### HR zone accuracy

Target HR zone comes from `plannedWorkout.targetHrZone`. HR zone is determined by comparing the workout's avg HR against standard Daniels zone boundaries as percentages of the user's max HR:

| Zone | % of Max HR |
|------|-------------|
| 1 (Easy) | 65-79% |
| 2 (Marathon) | 80-85% |
| 3 (Threshold) | 86-90% |
| 4 (Interval) | 91-95% |
| 5 (Repetition) | 96-100% |

- If planned HR zone, workout avg HR, or user max HR is null, HR factor defaults to 1.0 (not penalized for missing data)
- Zone match = 1.0, one zone off = 0.5, two+ zones off = 0.0

### Constants

`MatchConstants` class holds: factor weights (0.4, 0.4, 0.2), HR zone boundary percentages.

## WorkoutMatcher

Service in `com.runplanner.match` that handles the pairing logic.

**Method:** `Optional<WorkoutMatch> match(Workout workout)`

### Algorithm

1. If the workout is already matched (`existsByWorkout`), return empty
2. Find the user's active training plan via `TrainingPlanRepository.findByUserAndStatus(user, ACTIVE)`. If no active plan, return empty.
3. Get all planned workouts for the plan
4. Filter candidates: planned workouts within ±1 day of `workout.startedAt` (as LocalDate), not already matched (`existsByPlannedWorkout`), and not REST type
5. If no candidates, return empty
6. Score each candidate using `ComplianceScorer`
7. Pick the candidate with the highest compliance score
8. Create and save a `WorkoutMatch` with the score, return it

### Edge cases

- Multiple workouts on the same day competing for the same planned workout — first one matched wins (unique constraint on `planned_workout_id` enforces this)
- Workout with no active plan — silently returns empty
- Idempotent — calling match on an already-matched workout returns empty without side effects

### Dependencies

`WorkoutMatchRepository`, `TrainingPlanRepository`, `PlannedWorkoutRepository`, `ComplianceScorer`

## Testing Strategy

### ComplianceScorer (unit tests)

The bulk of testing — pure math:
- Perfect match (distance exact, pace exact, HR in zone) = 1.0
- Distance under target = proportional (80% distance = 0.8 * 0.4 weight)
- Distance over target = capped at 1.0
- Pace exactly at midpoint = 1.0 factor
- Pace at edge of range = 0.0 factor
- Pace outside range = 0.0 factor
- HR in target zone = 1.0
- HR one zone off = 0.5
- HR two+ zones off = 0.0
- Null avg HR = HR factor defaults to 1.0
- Null user max HR = HR factor defaults to 1.0
- Null planned HR zone = HR factor defaults to 1.0
- Null planned paces = pace factor defaults to 1.0
- REST planned workout = returns 0.0
- Weighted average combines correctly with known inputs
- Zero pace range (min == max) edge case

### WorkoutMatcher (unit tests, mocked deps)

- Happy path — workout matches best candidate
- Already matched workout = empty
- No active plan = empty
- No candidates within ±1 day = empty
- Multiple candidates — picks highest score
- REST planned workouts excluded from candidates
- Already-matched planned workouts excluded

## Package Structure

```
com.runplanner.match/
    WorkoutMatch.java          (entity)
    WorkoutMatchRepository.java (Spring Data JPA)
    ComplianceScorer.java      (pure scoring math, @Component)
    MatchConstants.java        (weights, HR zone boundaries)
    WorkoutMatcher.java        (matching service, @Service)
```
