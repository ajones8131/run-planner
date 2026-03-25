# Training Plans Feature Design

## Scope

Implements `training_plans` and `planned_workouts` domains with Daniels periodization-based plan generation. A user explicitly requests plan generation for a goal race via `POST /api/v1/plans`. The generator produces a full day-by-day plan from today to race day using VDOT-derived paces. Revision tracking is built in from the start. No Claude AI integration (deferred). No workout matching (separate spec).

## Database Migrations

### V6 — `training_plans`

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

### V7 — `planned_workouts`

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

### Schema notes

- `status` is a string enum (ACTIVE, COMPLETED, ARCHIVED), enforced at service layer
- `revision` starts at 1, incremented on plan modifications
- `plan_revision` on planned workouts tracks which plan revision created/modified them
- `original_scheduled_date` preserved so adjustments can track drift from the original plan
- `day_of_week` uses ISO standard: 1=Monday through 7=Sunday
- `workout_type` values: EASY, LONG, MARATHON, THRESHOLD, INTERVAL, REPETITION, REST

## Entities

Both entities live in `com.runplanner.plan` — they're tightly coupled and a planned workout has no meaning outside a training plan.

### TrainingPlan

Follows existing Lombok + JPA conventions (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Entity`).

Fields: `id` (UUID, generated), `user` (ManyToOne lazy FK), `goalRace` (ManyToOne lazy FK), `startDate` (LocalDate, not null), `endDate` (LocalDate, not null), `status` (TrainingPlanStatus enum, default ACTIVE), `revision` (int, default 1), `createdAt` (Instant, default now).

### TrainingPlanStatus

Enum: `ACTIVE`, `COMPLETED`, `ARCHIVED`.

### PlannedWorkout

Fields: `id` (UUID, generated), `trainingPlan` (ManyToOne lazy FK), `weekNumber` (int), `dayOfWeek` (int, 1-7 ISO), `scheduledDate` (LocalDate), `workoutType` (WorkoutType enum), `targetDistanceMeters` (double), `targetPaceMinPerKm` (Double, nullable for REST days), `targetPaceMaxPerKm` (Double, nullable), `targetHrZone` (Integer, nullable), `notes` (String, nullable), `originalScheduledDate` (LocalDate), `planRevision` (int, default 1).

### WorkoutType

Enum: `EASY`, `LONG`, `MARATHON`, `THRESHOLD`, `INTERVAL`, `REPETITION`, `REST`.

Maps to `TrainingZone` for pace calculation:
- EASY, LONG → TrainingZone.E
- MARATHON → TrainingZone.M
- THRESHOLD → TrainingZone.T
- INTERVAL → TrainingZone.I
- REPETITION → TrainingZone.R
- REST → no paces (null)

## Repositories

### TrainingPlanRepository

- `Optional<TrainingPlan> findByUserAndStatus(User, TrainingPlanStatus)` — find active plan (at most one ACTIVE per user)
- `List<TrainingPlan> findAllByUserOrderByCreatedAtDesc(User)` — list all plans
- `Optional<TrainingPlan> findByIdAndUser(UUID, User)` — single plan with ownership check
- `boolean existsByUserAndStatus(User, TrainingPlanStatus)` — check if active plan exists

### PlannedWorkoutRepository

- `List<PlannedWorkout> findAllByTrainingPlanOrderByScheduledDate(TrainingPlan)` — all workouts in a plan
- `List<PlannedWorkout> findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(TrainingPlan, LocalDate, LocalDate)` — workouts in a date range

## TrainingPlanGenerator

Pure Java class with `@Component` in `com.runplanner.plan`. Deterministic Daniels periodization logic.

**Input:** VDOT score (double), goal race distance (meters), race date (LocalDate), start date (LocalDate)

**Output:** `List<PlannedWorkout>` (unsaved, no IDs or plan association)

### Periodization phases

| Phase | % of total weeks | Focus |
|-------|-----------------|-------|
| Base (Phase I) | 25% | Easy runs, build mileage |
| Quality (Phase II) | 25% | Add threshold + interval work |
| Peak (Phase III) | 25% | Race-specific work, highest intensity |
| Taper (Phase IV) | 25% | Reduce volume, maintain intensity |

**Short plan compression:**
- >= 8 weeks: standard 25/25/25/25 split
- 4-7 weeks: compress proportionally, minimum 1 week per phase
- < 4 weeks: skip base and quality, go straight to peak + taper

### Weekly templates (day-of-week workout assignments)

| Day (ISO) | Base | Quality | Peak | Taper |
|-----------|------|---------|------|-------|
| Mon (1) | REST | REST | REST | REST |
| Tue (2) | EASY | THRESHOLD | INTERVAL | EASY |
| Wed (3) | EASY | EASY | EASY | EASY |
| Thu (4) | EASY | INTERVAL | THRESHOLD | THRESHOLD |
| Fri (5) | REST | REST | REST | REST |
| Sat (6) | LONG | LONG | LONG | LONG |
| Sun (7) | EASY | EASY | MARATHON | EASY |

### Mileage progression

- Weekly target mileage derived from VDOT (higher VDOT = more weekly km)
- Base phase: ramp from 70% to 100% of target
- Quality phase: hold at 100%
- Peak phase: hold at 100-105%
- Taper: decrease from 100% to 60% in final week

### Distance distribution

Daniels proportions within each week:
- Long run: ~25-30% of weekly total
- Quality sessions (threshold, interval, marathon, repetition): ~15-20% each
- Easy runs: split the remainder
- REST: 0 distance

### Pace ranges

Calculated using existing `TrainingPaceCalculator.calculate(vdotScore)` which returns `VdotScore` with `Map<TrainingZone, PaceRange>`. The generator maps `WorkoutType` to `TrainingZone` to look up min/max pace per km.

### Constants

`PlanConstants` class holds: phase percentage splits, weekly templates per phase, mileage scaling factors by VDOT range, distance distribution ratios, minimum weeks per phase.

## TrainingPlanService

Service in `com.runplanner.plan`.

### generate(User, UUID goalRaceId)

1. Validates the goal race exists and belongs to the user (404 if not)
2. Checks no ACTIVE plan exists for the user (409 Conflict if so)
3. Gets effective VDOT via `VdotHistoryService.getEffectiveVdot()` (400 if no VDOT)
4. Creates `TrainingPlan` entity (status=ACTIVE, revision=1, start=today, end=race date)
5. Calls `TrainingPlanGenerator` to produce planned workouts
6. Associates planned workouts with the saved plan
7. Saves everything and returns the plan with workouts

### findActive(User)

Returns the active plan with its planned workouts. Throws 404 if no active plan.

### findById(User, UUID)

Returns a specific plan with its planned workouts. Throws 404 if not found or not owned.

### findAll(User)

Lists all plans (without workouts) for the user, ordered by creation date descending.

### findPlannedWorkouts(User, UUID planId, LocalDate from, LocalDate to)

Returns planned workouts for a plan, optionally filtered by date range. Throws 404 if plan not found or not owned.

### archive(User, UUID planId)

Sets plan status to ARCHIVED. Throws 400 if plan is not ACTIVE. Throws 404 if not found or not owned. Returns 204.

### Dependencies

`TrainingPlanRepository`, `PlannedWorkoutRepository`, `TrainingPlanGenerator`, `GoalRaceRepository`, `VdotHistoryService`, `TrainingPaceCalculator`

## REST Endpoints

Controller at `/api/v1/plans`.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/plans` | Create a training plan for the given goal race. Body: `{ "goalRaceId": "uuid" }`. Returns 201. 409 if active plan exists. 400 if no VDOT. |
| `GET` | `/api/v1/plans` | List all plans for current user (without workouts). |
| `GET` | `/api/v1/plans/active` | Get the active plan with all planned workouts. 404 if none. |
| `GET` | `/api/v1/plans/{id}` | Get a specific plan with all planned workouts. |
| `GET` | `/api/v1/plans/{id}/workouts` | List planned workouts for a plan. Optional `?from=` and `?to=` date filters. |
| `DELETE` | `/api/v1/plans/{id}` | Archive a plan. 204 on success. 400 if not ACTIVE. |

### DTOs

**`CreatePlanRequest`** — record with `@NotNull UUID goalRaceId`

**`TrainingPlanResponse`** — `id`, `goalRaceId`, `startDate`, `endDate`, `status`, `revision`, `createdAt`, `workouts` (List<PlannedWorkoutResponse>, nullable — included on single-plan endpoints, excluded on list). Static `from(TrainingPlan)` and `from(TrainingPlan, List<PlannedWorkout>)` factory methods.

**`PlannedWorkoutResponse`** — `id`, `weekNumber`, `dayOfWeek`, `scheduledDate`, `workoutType`, `targetDistanceMeters`, `targetPaceMinPerKm`, `targetPaceMaxPerKm`, `targetHrZone`, `notes`, `planRevision`. Static `from(PlannedWorkout)` factory method.

## Testing Strategy

### TrainingPlanGenerator (unit tests)

The bulk of testing — this is where the periodization logic lives:
- Correct number of weeks from start to race date
- Phase assignment (base/quality/peak/taper) with correct proportions
- Short plan compression (< 8 weeks, < 4 weeks)
- Weekly template matches expected workout types per phase
- Mileage progression across phases (ramp, hold, taper)
- Distance distribution within a week (long run proportion, quality proportion)
- Pace ranges correctly derived from VDOT via TrainingPaceCalculator
- REST days have null paces and zero distance
- `originalScheduledDate` equals `scheduledDate` on initial generation
- All `planRevision` values are 1

### TrainingPlanService (unit tests, mocked deps)

- Generate happy path (creates plan, saves workouts)
- Generate with existing active plan (409)
- Generate with no VDOT (400)
- Generate with nonexistent/foreign goal race (404)
- findActive returns plan with workouts
- findActive with no active plan (404)
- findById with ownership check (404 for foreign plan)
- archive happy path
- archive non-ACTIVE plan (400)

### TrainingPlanController (MockMvc integration tests)

- POST /plans returns 201 with plan
- POST /plans returns 409 when active exists
- GET /plans returns list
- GET /plans/active returns plan with workouts
- GET /plans/{id} returns plan
- GET /plans/{id}/workouts returns workouts, with and without date filters
- DELETE /plans/{id} returns 204
- All endpoints return 401 unauthenticated

### PlanConstants (unit tests)

- Phase percentages sum to 1.0
- Weekly templates have 7 days each
- Distribution ratios are valid

## Package Structure

```
com.runplanner.plan/
    TrainingPlan.java            (entity)
    TrainingPlanStatus.java      (enum)
    PlannedWorkout.java          (entity)
    WorkoutType.java             (enum)
    TrainingPlanRepository.java  (Spring Data JPA)
    PlannedWorkoutRepository.java (Spring Data JPA)
    TrainingPlanGenerator.java   (pure periodization logic, @Component)
    PlanConstants.java           (phase/mileage/distribution constants)
    TrainingPlanService.java     (service)
    TrainingPlanController.java  (REST controller)
    dto/
        CreatePlanRequest.java
        TrainingPlanResponse.java
        PlannedWorkoutResponse.java
```
