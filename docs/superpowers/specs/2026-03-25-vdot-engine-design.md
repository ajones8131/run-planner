# VDOT Engine — Design Spec
*Date: 2026-03-25*

## Overview

A pure math engine that implements Jack Daniels' VDOT methodology for the run-planner backend. Calculates VDOT scores from race performances, predicts race times, and derives training zone paces. Paired with a persistence layer (`vdot_history`) and REST endpoints for managing VDOT history.

The engine is stateless with no Spring dependencies — consumed by other packages (health sync, plan generation, adjustment) that handle trigger logic and orchestration.

---

## Decisions

- **Regression formulas** over lookup tables — continuous values for any VDOT, no table maintenance
- **VDOT range 30–85** — matches standard Daniels tables
- **Metric-only internally** — paces always in min/km; unit conversion happens at the display/DTO layer
- **Pure math engine** — no trigger logic, no persistence, no user context; calling services handle when to invoke it
- **Pace ranges for all zones** — every zone returns min/max pace; width conveys strictness
- **Separate classes per responsibility** — `VdotCalculator`, `TrainingPaceCalculator`, `VdotConstants`, `PaceRange`

---

## Package Structure

Package: `com.runplanner.vdot`

### Pure math classes (no Spring annotations, no dependencies outside the package)

**`VdotConstants`**
- Enum `TrainingZone` with values: `E`, `M`, `T`, `I`, `R` — each with display name and purpose
- Static constants: `MIN_VDOT = 30`, `MAX_VDOT = 85`
- Formula coefficients as named constants

**`PaceRange`** (record)
- `double minPaceMinPerKm` — faster pace (lower number)
- `double maxPaceMinPerKm` — slower pace (higher number)

**`VdotCalculator`**
- `double calculateVdot(double distanceMeters, double timeSeconds)` — VDOT score from a race performance
- `double predictRaceTime(double vdotScore, double distanceMeters)` — predicted finish time in seconds for a given VDOT and distance
- Throws `IllegalArgumentException` for inputs outside supported ranges

**`TrainingPaceCalculator`**
- `Map<TrainingZone, PaceRange> calculate(double vdotScore)` — returns min/max pace (min/km) for all five zones
- Throws `IllegalArgumentException` if VDOT is outside 30–85

**`VdotScore`** (record)
- `double score`
- `Map<TrainingZone, PaceRange> trainingPaces`
- Convenience wrapper assembled by higher-level services, not by the engine itself

---

## Formulas

### Oxygen cost of running

```
velocity = distanceMeters / timeMinutes    (meters per minute)
oxygenCost = -4.60 + 0.182258 * velocity + 0.000104 * velocity²
```

### VO2max (VDOT) from race performance

```
timeMinutes = timeSeconds / 60
pctVO2max = 0.8 + 0.1894393 * e^(-0.012778 * timeMinutes) + 0.2989558 * e^(-0.1932605 * timeMinutes)
vdot = oxygenCost / pctVO2max
```

### Race time prediction

Inverts the above — given a VDOT and distance, find the time where the formulas converge. Solved iteratively via binary search since there is no closed-form inverse.

### Training pace derivation

Each zone is defined by a %VO2max range:

| Zone | %VO2max range |
|------|--------------|
| E | 59–74% |
| M | 75–84% |
| T | 83–88% (intentional overlap with M) |
| I | 95–100% |
| R | 105–110% |

For each zone boundary: compute the equivalent oxygen cost from the %VO2max and the user's VDOT, solve for velocity, convert velocity to min/km pace.

---

## Persistence Layer

### Entity: `VdotHistory`

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| user | User (FK) | cascade |
| triggeringWorkoutId | UUID | nullable |
| triggeringSnapshotId | UUID | nullable |
| previousVdot | double | |
| newVdot | double | |
| calculatedAt | Instant | |
| flagged | boolean | true if delta > 5 points |
| accepted | boolean | false if flagged, true otherwise |

Constraint: exactly one of `triggeringWorkoutId` or `triggeringSnapshotId` is non-null per row.

**Note on triggering IDs:** These are opaque UUIDs stored for traceability — no foreign key constraints in the database. The `workouts` and `health_snapshots` tables do not exist yet and will be created by the health sync feature. The VDOT history layer stores the IDs without referential integrity so it can be built and tested independently.

**Initial VDOT:** When recording the first VDOT calculation for a user (no prior history), `previousVdot` is set to `0.0` to indicate no prior value.

### Migration: `V4__create_vdot_history.sql`

```sql
CREATE TABLE vdot_history (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    triggering_workout_id  UUID,
    triggering_snapshot_id UUID,
    previous_vdot   DOUBLE PRECISION NOT NULL,
    new_vdot        DOUBLE PRECISION NOT NULL,
    calculated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    flagged         BOOLEAN NOT NULL DEFAULT FALSE,
    accepted        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_single_trigger CHECK (
        (triggering_workout_id IS NOT NULL AND triggering_snapshot_id IS NULL) OR
        (triggering_workout_id IS NULL AND triggering_snapshot_id IS NOT NULL)
    )
);

CREATE INDEX idx_vdot_history_user_id ON vdot_history(user_id);
```

### Repository: `VdotHistoryRepository`

- `findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(User user)` — current effective VDOT
- `findAllByUserOrderByCalculatedAtDesc(User user)` — full history

### Service: `VdotHistoryService`

- `recordCalculation(User, double previousVdot, double newVdot, UUID triggeringWorkoutId, UUID triggeringSnapshotId)` — creates entry; auto-flags if abs(newVdot - previousVdot) > 5; sets `accepted = false` when flagged, `accepted = true` otherwise
- `getEffectiveVdot(User)` → `Optional<Double>` — most recent accepted entry's `newVdot`
- `acceptFlagged(User, UUID historyId)` — sets `accepted = true`; throws if not flagged or not owned by user
- `dismissFlagged(User, UUID historyId)` — effective VDOT unchanged; throws if entry is not flagged or not owned by user

---

## REST API

Controller: `VdotController` at `/api/v1/vdot`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/history` | List VDOT history for current user (ordered by calculatedAt desc) |
| `POST` | `/history/{id}/accept` | Accept a flagged VDOT change |
| `POST` | `/history/{id}/dismiss` | Dismiss a flagged VDOT change |

### Response DTOs

**`VdotHistoryResponse`** (record)
- `UUID id`
- `UUID triggeringWorkoutId`
- `UUID triggeringSnapshotId`
- `double previousVdot`
- `double newVdot`
- `Instant calculatedAt`
- `boolean flagged`
- `boolean accepted`

---

## Testing Strategy

### VdotCalculator tests
- Known race performances → expected VDOT scores verified against published Daniels tables (±0.5 tolerance)
  - e.g., 5K in 20:00 → ~42.2 VDOT, marathon in 3:00:00 → ~54.6 VDOT
- Boundary cases: VDOT 30 and 85 edges, very short and very long distances
- Round-trip: calculateVdot then predictRaceTime for same distance → original time (within tolerance)
- Invalid inputs → IllegalArgumentException

### TrainingPaceCalculator tests
- Known VDOT scores → zone paces verified against published Daniels tables (±2–3 sec/km tolerance)
- All five zones always present in result
- Min pace (faster) < max pace (slower) for every zone
- Higher VDOT → faster paces across all zones (monotonicity)
- VDOT outside 30–85 → IllegalArgumentException

### VdotHistoryService tests
- Normal calculation (delta ≤ 5) → flagged=false, accepted=true
- Large delta (>5 points) → flagged=true, accepted=false
- getEffectiveVdot returns most recent accepted entry, ignoring flagged-not-accepted
- Accept/dismiss on flagged entries updates correctly
- Accept/dismiss on non-flagged or other user's entries → error

### VdotController tests
- MockMvc integration tests for all three endpoints
- Auth enforcement — unauthenticated requests return 401
- User isolation — cannot accept/dismiss another user's entries
