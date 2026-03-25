# Workouts Feature Design

## Scope

Implements the `workouts` domain as defined in the run-planner system spec: entity, repository, service, read-only REST endpoints, and VDOT recalculation trigger wiring. No `workout_matches` (depends on training plans). No write endpoints (workouts are ingested via the future health sync pipeline).

## Database Migration (V5)

```sql
CREATE TABLE workouts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source              VARCHAR(50) NOT NULL,
    source_id           VARCHAR(255),
    started_at          TIMESTAMPTZ NOT NULL,
    distance_meters     DOUBLE PRECISION NOT NULL,
    duration_seconds    INTEGER NOT NULL,
    avg_hr              INTEGER,
    max_hr              INTEGER,
    elevation_gain      DOUBLE PRECISION
);

CREATE INDEX idx_workouts_user_id ON workouts(user_id);
CREATE INDEX idx_workouts_user_started_at ON workouts(user_id, started_at);
CREATE UNIQUE INDEX idx_workouts_source_source_id ON workouts(source, source_id);

ALTER TABLE vdot_history
    ADD CONSTRAINT fk_vdot_triggering_workout
    FOREIGN KEY (triggering_workout_id) REFERENCES workouts(id);
```

- `source` identifies the external system (e.g., "APPLE_HEALTH", "GARMIN")
- `source_id` is the external identifier within that system, nullable but unique per source
- Composite unique index on `(source, source_id)` for deduplication
- Composite index on `(user_id, started_at)` for the `?since=` query
- Adds FK from `vdot_history.triggering_workout_id` to `workouts.id`

## Entity

`Workout` in `com.runplanner.workout` package. Follows existing Lombok + JPA conventions (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Entity`).

Fields: `id` (UUID, generated), `user` (ManyToOne lazy FK), `source` (String, not null), `sourceId` (String, nullable), `startedAt` (Instant, not null), `distanceMeters` (Double, not null), `durationSeconds` (Integer, not null), `avgHr` (Integer, nullable), `maxHr` (Integer, nullable), `elevationGain` (Double, nullable).

## Repository

`WorkoutRepository extends JpaRepository<Workout, UUID>`:

- `findAllByUserAndStartedAtAfterOrderByStartedAtDesc(User, Instant)` — supports `?since=` filter
- `findAllByUserOrderByStartedAtDesc(User)` — full list
- `findByIdAndUser(UUID, User)` — single workout with ownership check
- `existsBySourceAndSourceId(String, String)` — deduplication check

## VdotRecalculationService

New service in `com.runplanner.vdot` that owns VDOT recalculation criteria evaluation. Extracted as a dedicated service so both workout save and future health-snapshot sync can reuse it.

**Method**: `Optional<VdotHistory> evaluate(Workout workout)`

**Recalculation criteria** (all must be met):
1. Duration >= 10 minutes (`MIN_DURATION_SECONDS = 600`)
2. Average HR >= 90% of user's max HR (`HR_THRESHOLD_PERCENT = 0.9`); skipped if either value is null
3. Distance within 5% of a standard race distance (5K, 10K, half marathon, marathon) or any of the user's active goal race distances (`DISTANCE_TOLERANCE = 0.05`)

When all criteria are met, calculates VDOT via `VdotCalculator.calculateVdot()`, retrieves the current effective VDOT via `VdotHistoryService.getEffectiveVdot()`, and records the result via `VdotHistoryService.recordCalculation()` with the workout ID as the triggering source.

**New constants** added to `VdotConstants`:
- `MIN_DURATION_SECONDS = 600`
- `HR_THRESHOLD_PERCENT = 0.9`
- `DISTANCE_TOLERANCE = 0.05`

**Dependencies**: `VdotCalculator`, `VdotHistoryService`, `GoalRaceRepository`.

## WorkoutService

Service in `com.runplanner.workout`.

- `save(Workout)` — persists the workout, then calls `VdotRecalculationService.evaluate()`. Returns the saved entity. Not exposed via REST; called by future health sync.
- `findAll(User, Instant since)` — returns `List<WorkoutResponse>`. Uses the `since` filter when non-null.
- `findById(User, UUID)` — returns `WorkoutResponse` or throws 404.
- `existsBySourceAndSourceId(String, String)` — delegates to repository for deduplication.

## REST Endpoints

Read-only controller at `/api/v1/workouts`.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/workouts` | List workouts for current user. Optional `?since=` ISO timestamp filter. |
| `GET` | `/api/v1/workouts/{id}` | Get a single workout by ID. 404 if not found or not owned. |

**Response DTO** (`WorkoutResponse` record): `id`, `startedAt`, `distanceMeters`, `durationSeconds`, `avgHr`, `maxHr`, `elevationGain`, `source`, `sourceId`. Static `from(Workout)` factory method.

## Testing Strategy

- **VdotRecalculationService**: unit tests with mocked dependencies covering all gate combinations (duration too short, HR missing, HR below threshold, distance mismatch, all criteria met, standard distances, custom goal race distances)
- **WorkoutService**: unit tests verifying save triggers recalculation, read methods delegate correctly, ownership enforcement on findById
- **WorkoutController**: integration tests with MockMvc and JWT auth for both endpoints, `?since=` filter behavior, 404 on missing/unowned workout, user isolation
- **WorkoutRepository**: verify deduplication query works correctly

## Package Structure

```
com.runplanner.workout/
    Workout.java            (entity)
    WorkoutRepository.java  (Spring Data JPA)
    WorkoutService.java     (service)
    WorkoutController.java  (REST controller)
    dto/
        WorkoutResponse.java (response record)

com.runplanner.vdot/
    VdotRecalculationService.java  (new)
    VdotConstants.java             (updated with new constants)
```
