# Run Planner — Design Spec
*Date: 2026-03-24*

## Overview

A personalized iOS running coach app that generates structured training plans and tracks progress using data ingested from Apple Health. Plans are grounded in Jack Daniels' VDOT methodology — a mathematical framework that derives training paces from VO2 max — supplemented by Claude AI for structural plan decisions and adjustments.

Built primarily for an advanced runner (15 years experience) with eventual expansion to casual and intermediate runners. The iOS app is a thin client; all business logic lives in a Java backend.

---

## Architecture

### Two deployable units

**Flutter iOS app**
- Reads Apple Health data via HealthKit: workouts, heart rate history, VO2 max estimate
- Displays training plans, individual workout details, and progress
- Sends health data snapshots to the backend via REST
- No business logic on-device
- Syncs on app open (background sync deferred to coaching notification feature)

**Spring Boot monolith (Java 25, PostgreSQL)**
- Owns all plan generation, VDOT math, AI integration, and data storage
- Exposes a versioned REST/JSON API (`/api/v1/...`) consumed by the Flutter app
- Deployed locally during initial development; Flutter app points to localhost/LAN
- Internal package structure:
  - `health` — ingests and stores Apple Health data (workouts, HR, VO2 max history)
  - `vdot` — pure math engine: VDOT score calculation, training zones, target paces for all workout types
  - `plan` — generates and manages training plans; owns the weekly structure from today to race day
  - `adjustment` — compares completed workouts to planned workouts; modifies upcoming weeks based on performance
  - `ai` — wraps the Claude API (Anthropic); used by `plan` and `adjustment` for structural decisions
  - `user` — user profile, goal races, JWT authentication

---

## Supported Race Distances

- 5K (5,000m), 10K (10,000m), half marathon (21,097m), marathon (42,195m)
- Custom user-defined distances (stored as meters)

---

## Training Methodology

**Jack Daniels' VDOT system**

A single VDOT score is derived from the user's VO2 max (sourced from Apple Health) or from a recent race/time trial performance. The VDOT score maps to precise target paces for five training zone types:

| Zone | Type | Purpose |
|------|------|---------|
| E | Easy | Aerobic base, recovery |
| M | Marathon pace | Race-specific aerobic work |
| T | Threshold | Lactate threshold improvement |
| I | Interval | VO2 max development |
| R | Repetition | Speed and economy |

**VDOT recalculation triggers:**
A recalculation is triggered when an ingested workout meets all of the following criteria:
- Average HR ≥ 90% of the user's recorded max HR
- Distance is within 5% of a supported race distance (5K, 10K, half, marathon) or a user-defined custom race distance
- Duration is at least 10 minutes

On recalculation, the new VDOT score and the triggering workout ID are recorded in `vdot_history`. The `users.current_vdot` field is updated to reflect the new score. If a recalculation produces a VDOT change of more than 5 points in a single workout, it is flagged for review rather than auto-applied (likely a data anomaly).

**AI role (Claude API)**
Claude is used for structural plan decisions — macro weekly mileage progression, taper timing, workout variety balance — and for determining whether and how to adjust plans after performance deviations. Prompts return structured JSON. The VDOT math engine provides all target paces; Claude does not override pace calculations. All Claude calls are non-blocking with pure VDOT math fallback if the call fails.

Future features (not in initial scope): natural language coaching commentary, conversational onboarding, background sync with push notifications.

---

## Authentication

JWT-based authentication with email and password. Tokens are included in the `Authorization: Bearer <token>` header on all API calls. The `users` table stores a hashed password (bcrypt) and email as the login identity.

**Token lifecycle:**
- Access token TTL: 1 hour
- Refresh token TTL: 30 days
- Refresh tokens are stored in a `refresh_tokens` table (`id`, `user_id`, `token_hash`, `expires_at`, `revoked`) to support revocation
- On logout or password change, all refresh tokens for the user are revoked
- Endpoint: `POST /api/v1/auth/refresh` — accepts a refresh token, returns a new access token and refresh token (rotation)

---

## Data Model

```plantuml
@startuml
entity users {
    * id : uuid <<PK>>
    --
    email : varchar <<unique>>
    password_hash : varchar
    name : varchar
    date_of_birth : date
    resting_hr : int
    max_hr : int
    current_vdot : float
    preferred_units : varchar
    last_synced_at : timestamp
}

entity goal_races {
    * id : uuid <<PK>>
    --
    * user_id : uuid <<FK>>
    distance_meters : int
    distance_label : varchar
    race_date : date
    goal_finish_seconds : int
    status : varchar <<ACTIVE|COMPLETED|ARCHIVED>>
}

entity health_snapshots {
    * id : uuid <<PK>>
    --
    * user_id : uuid <<FK>>
    vo2max_estimate : float
    resting_hr : int
    recorded_at : timestamp
}

entity workouts {
    * id : uuid <<PK>>
    --
    * user_id : uuid <<FK>>
    apple_health_uuid : varchar <<unique>>
    started_at : timestamp
    distance_meters : float
    duration_seconds : int
    avg_hr : int
    max_hr : int
    elevation_gain : float
    source : varchar
}

entity vdot_history {
    * id : uuid <<PK>>
    --
    * user_id : uuid <<FK>>
    triggering_workout_id : uuid <<FK, nullable>>
    triggering_snapshot_id : uuid <<FK, nullable>>
    previous_vdot : float
    new_vdot : float
    calculated_at : timestamp
    flagged : boolean
    accepted : boolean
}

entity training_plans {
    * id : uuid <<PK>>
    --
    * user_id : uuid <<FK>>
    * goal_race_id : uuid <<FK>>
    start_date : date
    end_date : date
    status : varchar <<ACTIVE|COMPLETED|ARCHIVED>>
    revision : int
}

entity planned_workouts {
    * id : uuid <<PK>>
    --
    * training_plan_id : uuid <<FK>>
    week_number : int
    day_of_week : int
    scheduled_date : date
    workout_type : varchar <<EASY|MARATHON|THRESHOLD|INTERVAL|REPETITION|REST>>
    target_distance_meters : float
    target_pace_min_per_km : float
    target_pace_max_per_km : float
    target_hr_zone : int
    notes : text
    original_scheduled_date : date
    plan_revision : int
}

entity workout_matches {
    * id : uuid <<PK>>
    --
    * planned_workout_id : uuid <<FK>>
    * workout_id : uuid <<FK>>
    compliance_score : float
}

users ||--o{ goal_races : "has"
users ||--o{ health_snapshots : "has"
users ||--o{ workouts : "logs"
users ||--o{ training_plans : "has"
users ||--o{ vdot_history : "has"
goal_races ||--o| training_plans : "drives"
training_plans ||--o{ planned_workouts : "contains"
planned_workouts ||--o| workout_matches : "matched by"
workouts ||--o| workout_matches : "fulfills"
workouts |o--o{ vdot_history : "triggers"
health_snapshots |o--o{ vdot_history : "triggers"
@enduml
```

### Status enumerations

**`goal_races.status`**
- `ACTIVE` — current goal race, plan is live
- `COMPLETED` — race date has passed
- `ARCHIVED` — manually dismissed by user

**`training_plans.status`**
- `ACTIVE` — currently in use (one per user at a time)
- `COMPLETED` — race date passed
- `ARCHIVED` — superseded by a new plan or manually dismissed

Only one `training_plan` per user may have status `ACTIVE` at a time (enforced at the service layer).

### workout_matches constraints

`workout_matches` enforces unique constraints at the database level on both `planned_workout_id` and `workout_id` independently — a planned workout can match at most one real workout, and a real workout can satisfy at most one planned workout. Matching is idempotent: re-running the matcher on already-matched workouts produces no duplicate rows.

### Compliance score formula

`compliance_score` is a value between 0.0 and 1.0 calculated as a weighted average of three factors:

| Factor | Weight | Calculation |
|--------|--------|-------------|
| Distance completion | 40% | `min(actual_distance / target_distance, 1.0)` |
| Pace accuracy | 40% | `1.0 - clamp(|actual_pace - target_pace_mid| / target_pace_range, 0, 1)` |
| HR zone accuracy | 20% | `1.0` if avg HR within target zone, else `0.5` if within one zone, else `0.0` |

A `compliance_score` of 1.0 is a perfect match. A score below 0.6 is considered a significant deviation.

### Plan adjustment thresholds

The adjustment engine triggers a plan recalculation when any of the following conditions are met:
- Two or more consecutive planned workouts have `compliance_score < 0.6`
- A long run (the week's longest planned workout) is missed entirely (no match found within ±1 day)
- The user's VDOT score changes by more than 2 points (indicating meaningful fitness change)

Minor adjustments (pace nudges only, no structural change) are applied when:
- Three of the last five workouts have `compliance_score` between 0.6 and 0.75

### Unmatched workouts

When a synced workout has no matching `planned_workout` within ±1 day of `scheduled_date`, it is stored in `workouts` without a `workout_matches` row. It still participates in VDOT recalculation if it meets the criteria. The adjustment engine treats an unmatched workout on the same day as a scheduled non-REST workout as a missed workout for threshold evaluation purposes.

### VO2 max snapshot selection

When calculating initial VDOT from `health_snapshots`, the most recent snapshot by `recorded_at` is used. If multiple snapshots share the same timestamp, the highest `vo2max_estimate` is selected. `vdot_history` rows triggered by a snapshot reference `triggering_snapshot_id` (not `triggering_workout_id`); exactly one of the two FK fields is non-null per row.

### Claude prompt contract

**Plan generation prompt inputs (sent as JSON):**
```json
{
  "current_vdot": 52.3,
  "weeks_to_race": 16,
  "goal_finish_seconds": 5400,
  "race_distance_meters": 21097,
  "recent_weekly_mileage_km": 55.0,
  "max_hr": 185
}
```

**Required Claude response fields:**
```json
{
  "weekly_plans": [
    {
      "week_number": 1,
      "total_distance_km": 55.0,
      "workouts": [
        { "day_of_week": 1, "workout_type": "EASY", "distance_km": 10.0 },
        { "day_of_week": 3, "workout_type": "THRESHOLD", "distance_km": 8.0 },
        { "day_of_week": 6, "workout_type": "LONG", "distance_km": 18.0 },
        { "day_of_week": 0, "workout_type": "REST", "distance_km": 0 }
      ]
    }
  ]
}
```

Claude provides distance and workout type per day; the VDOT engine then calculates target pace ranges and HR zones. If Claude's response is missing required fields or fails to parse, the math fallback generates a standard Daniels periodization pattern (base → quality → peak → taper).

---

## API Surface

All endpoints are prefixed `/api/v1/`. All requests (except auth endpoints) require `Authorization: Bearer <token>`.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Create account; returns access + refresh tokens |
| `POST` | `/auth/login` | Authenticate; returns access + refresh tokens |
| `POST` | `/auth/refresh` | Exchange refresh token for new token pair |
| `POST` | `/auth/logout` | Revoke all refresh tokens for the current user |
| `GET` | `/users/me` | Get current user profile |
| `PATCH` | `/users/me` | Update profile (name, DOB, max HR, resting HR, units) |
| `POST` | `/goal-races` | Create a goal race |
| `GET` | `/goal-races` | List goal races for current user |
| `PATCH` | `/goal-races/{id}` | Update a goal race (date, goal time, status) |
| `POST` | `/health/sync` | Ingest Apple Health data (workouts + snapshots); triggers matching, adjustment, and VDOT recalculation; returns updated active plan |
| `GET` | `/plans/active` | Get active training plan with all planned workouts |
| `GET` | `/plans/{id}` | Get a specific plan |
| `GET` | `/plans/{id}/workouts` | List all planned workouts in a plan |
| `GET` | `/workouts` | List ingested workouts (supports `?since=` timestamp filter) |
| `GET` | `/workouts/{id}` | Get a single workout with its match and compliance score |
| `GET` | `/vdot/history` | List VDOT history for current user |
| `POST` | `/vdot/history/{id}/accept` | Accept a flagged VDOT change |
| `POST` | `/vdot/history/{id}/dismiss` | Dismiss a flagged VDOT change |

---

## Data Flow

### Onboarding
1. User registers with email and password; receives JWT
2. User enters profile (date of birth, max HR) and goal race (distance, date, goal finish time)
3. Flutter app reads VO2 max estimate and recent workouts from Apple Health → sends to `/api/v1/health/sync`
4. Backend calculates initial VDOT from VO2 max, or from best recent race-like workout if available
5. Backend calls Claude to determine macro plan structure (weekly mileage progression, workout distribution, taper)
6. Backend generates full training plan using VDOT paces for each workout type and Claude's structure
7. Plan returned to Flutter app for display

### Ongoing sync (on app open)
1. Flutter app reads new workouts from Apple Health since `users.last_synced_at` → sends to `/api/v1/health/sync`
2. Backend stores raw workouts, deduplicating by `apple_health_uuid`; updates `last_synced_at`
3. Backend attempts to match each new workout to a `planned_workout` within ±1 day of `scheduled_date`
4. `compliance_score` is calculated for each match
5. Adjustment engine evaluates recent compliance against thresholds; if triggered, calls Claude to decide restructure vs. pace adjustment
6. If VDOT recalculation criteria are met, new VDOT is calculated and `vdot_history` is recorded
7. Updated plan returned to app (adjustments are applied before response, not deferred)

### VDOT recalculation
Triggered during sync when an ingested workout meets all recalculation criteria (see Training Methodology). The new VDOT updates `users.current_vdot` and is recorded in `vdot_history`. All remaining `planned_workouts` in the active plan have their target paces recalculated using the new VDOT score.

---

## Error Handling

- **Apple Health sync** — best-effort; partial data accepted and stored, never a hard failure; duplicate workouts silently skipped via `apple_health_uuid` uniqueness
- **Missing VO2 max** — falls back to prompting the user to enter a recent race time or estimated goal pace to seed initial VDOT
- **Claude API failure** — non-blocking; backend generates plan using pure VDOT math rules as fallback; retries Claude enrichment asynchronously on next sync
- **VDOT anomaly** — changes >5 points are written to `vdot_history` with `flagged = true` and `accepted = false`; `users.current_vdot` is not updated; the app surfaces a prompt asking the user to accept or dismiss; on acceptance `accepted = true` and `current_vdot` is updated; on dismissal the row is kept for audit but `current_vdot` is unchanged; flagged rows are never auto-resolved
- **Active plan conflict** — service layer rejects creation of a second `ACTIVE` plan; existing plan must be archived first

---

## Testing Strategy

- **VDOT engine** — thorough unit test coverage; pure math with deterministic inputs/outputs; edge cases include very low/high VDOT scores, missed training weeks, proximity to race day, anomaly flagging
- **Compliance score** — unit tested with known inputs and expected outputs for all three factors and boundary conditions
- **Adjustment logic** — unit tested with stubbed Claude seam; covers over-performance, under-performance, missed long runs, VDOT-triggered adjustment, and minor pace nudge scenarios
- **REST API** — integration tests for all `/api/v1/` endpoints and the Apple Health ingestion pipeline, including deduplication behavior
- **Claude layer** — isolated behind an interface; stubbed in all automated tests; real API calls limited to manual/exploratory testing
- **Flutter** — widget tests for key screens (plan view, workout detail); no business logic to unit test

---

## Out of Scope (Initial Release)

- Natural language coaching commentary
- Conversational onboarding assistant
- Background sync and push notifications
- Beginner/intermediate plan tiers (advanced runner profile only)
- Android support
- Web dashboard
- Cloud deployment (local only initially)
