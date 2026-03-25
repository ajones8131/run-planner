# Health Sync Feature Design

## Scope

Implements `POST /api/v1/health/sync` endpoint that ingests workouts and health snapshots from the Flutter client. Orchestrates existing services: workout deduplication and save, workout matching, VDOT recalculation (from workouts and from VO2max snapshots). Also implements the `HealthSnapshot` entity. Returns a sync summary. No plan adjustment engine (separate feature).

## Database Migration (V9)

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

- Both `vo2max_estimate` and `resting_hr` are nullable — a snapshot might have one or both
- Composite index on `(user_id, recorded_at)` supports the "most recent snapshot" query
- Adds FK from `vdot_history.triggering_snapshot_id` to `health_snapshots.id`

## HealthSnapshot Entity

`HealthSnapshot` in `com.runplanner.health` package.

Fields: `id` (UUID, generated), `user` (ManyToOne lazy FK), `vo2maxEstimate` (Double, nullable), `restingHr` (Integer, nullable), `recordedAt` (Instant, not null).

## HealthSnapshotRepository

- `Optional<HealthSnapshot> findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(User)` — most recent snapshot with VO2max
- `List<HealthSnapshot> findAllByUserOrderByRecordedAtDesc(User)` — list all

## Request DTO

`HealthSyncRequest` record in `com.runplanner.health.dto`:

```
{
  "workouts": [
    {
      "source": "APPLE_HEALTH",
      "sourceId": "abc-123",
      "startedAt": "2026-03-25T08:00:00Z",
      "distanceMeters": 10000.0,
      "durationSeconds": 3000,
      "avgHr": 155,
      "maxHr": 172,
      "elevationGain": 45.0
    }
  ],
  "healthSnapshots": [
    {
      "vo2maxEstimate": 48.5,
      "restingHr": 52,
      "recordedAt": "2026-03-25T06:00:00Z"
    }
  ]
}
```

Both `workouts` and `healthSnapshots` arrays are optional (null or empty treated as no data). Nested records `WorkoutSyncItem` and `HealthSnapshotSyncItem` with Jakarta validation annotations on required fields.

## Response DTO

`HealthSyncResponse` record:

```json
{
  "workoutsSaved": 3,
  "workoutsSkipped": 1,
  "workoutsMatched": 2,
  "snapshotsSaved": 1,
  "vdotUpdated": true
}
```

## REST Endpoint

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/health/sync` | Ingest workouts + snapshots, run matching and VDOT recalculation, return sync summary. 200 on success. |

Controller in `com.runplanner.health`.

## HealthSyncService

Orchestrator in `com.runplanner.health`. Linear flow, delegates to existing services.

**Method:** `HealthSyncResponse sync(User user, HealthSyncRequest request)`

### Flow

1. **Ingest workouts:** For each `WorkoutSyncItem`, check `WorkoutService.existsBySourceAndSourceId()` for dedup. If not duplicate, build `Workout` entity and call `WorkoutService.save()` (which already triggers VDOT recalculation automatically via `VdotRecalculationService`). Track saved vs skipped counts.

2. **Match new workouts:** For each newly saved workout, call `WorkoutMatcher.match()`. Track matched count.

3. **Ingest health snapshots:** Save each `HealthSnapshotSyncItem` as a `HealthSnapshot` entity via `HealthSnapshotRepository`. Track saved count.

4. **VDOT from VO2max:** After saving snapshots, find the most recent snapshot with a non-null `vo2maxEstimate` via `HealthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc()`. If found, get the current effective VDOT via `VdotHistoryService.getEffectiveVdot(user)`. If the VO2max differs from the current effective VDOT (or no existing VDOT), record via `VdotHistoryService.recordCalculation(user, previousVdot, newVdot, null, snapshotId)` where `previousVdot` is the effective VDOT or `0.0` if none exists and `newVdot` is the `vo2maxEstimate`. VO2max ≈ VDOT in Daniels' model — `vo2maxEstimate` is used directly as the VDOT score, no conversion needed.

5. **Update `lastSyncedAt`:** Set `user.lastSyncedAt = Instant.now()` and save via `UserRepository`.

6. **Return summary** with counts and whether VDOT was updated.

### Dependencies

`WorkoutService`, `WorkoutMatcher`, `HealthSnapshotRepository`, `VdotHistoryService`, `UserRepository`

### Error handling

Best-effort — if one workout fails to save, continue with the rest. Workout duplicates are silently skipped via `source`/`sourceId` check. Health snapshots are not deduplicated — duplicate snapshots are harmless since only the most recent with a VO2max value is used for VDOT calculation. The overall sync never fails hard.

## Testing Strategy

### HealthSyncService (unit tests, mocked deps)

- Sync with workouts only — saves non-duplicates, skips duplicates, counts correct
- Sync with snapshots only — saves all, counts correct
- Sync with both workouts and snapshots — full flow
- Deduplication — workout with existing sourceId skipped
- Matching — newly saved workouts passed to WorkoutMatcher
- VDOT from VO2max — most recent snapshot triggers VdotHistory recording
- VDOT from VO2max — skipped when no vo2maxEstimate present
- VDOT from VO2max — skipped when VO2max equals current effective VDOT
- Updates lastSyncedAt on user
- Empty request (no workouts, no snapshots) — returns zeros, still updates lastSyncedAt
- Null lists in request — treated as empty, no errors

### HealthSyncController (MockMvc integration tests)

- POST /health/sync returns 200 with summary
- POST /health/sync with empty body returns 200
- POST /health/sync unauthenticated returns 401

## Package Structure

```
com.runplanner.health/
    HealthSnapshot.java           (entity)
    HealthSnapshotRepository.java (Spring Data JPA)
    HealthSyncService.java        (orchestrator)
    HealthSyncController.java     (REST controller)
    dto/
        HealthSyncRequest.java    (request with nested WorkoutSyncItem, HealthSnapshotSyncItem)
        HealthSyncResponse.java   (sync summary)
```
