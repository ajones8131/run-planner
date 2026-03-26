# Flutter Frontend Design

## Overview

A Flutter iOS app that serves as a thin client for the run-planner backend. The app provides account creation, goal race setup, training plan generation, daily dashboard, and Apple Health integration. Designed iOS-first with cross-platform expansion in mind.

## Design Decisions

- **Platform:** Flutter (Dart) — cross-platform, Dart is closest to Java for a backend developer learning mobile
- **Architecture:** Thin client — all business logic lives on the backend, the app displays and syncs
- **Navigation:** Tab-based with bottom navigation bar (4 tabs)
- **Aesthetic:** Clean and minimal — every piece of data must earn its place on screen
- **Health sync:** Silent on app open, with subtle "last synced" indicator
- **VDOT review:** Notification-style banner on dashboard for flagged changes
- **V1 scope:** Full loop from account creation through daily use

## App Structure

### Onboarding Flow (linear, one-time)

Full-screen pages shown in sequence before the main app:

1. **Welcome / Register** — Email + password form with "Already have an account? Log in" link.
2. **Profile Setup** — Name, date of birth, max heart rate (optional, can be estimated), preferred units (km/mi).
3. **HealthKit Permissions** — Explanation of what data is needed and why, then iOS permission dialog.
4. **Initial Sync** — Loading screen while Apple Health data is read and sent to the backend. Backend calculates initial VDOT.
5. **Goal Race Wizard** — Guided setup:
   - Step 1: Race distance (5K, 10K, Half Marathon, Marathon, or custom)
   - Step 2: Race date (date picker)
   - Step 3: Goal finish time (hours:minutes picker)
   - Step 4: Confirmation showing VDOT, generated training paces, and "Generate Plan" button
6. **Plan Generated** — Summary screen, tap to enter main app.

After onboarding, these screens are not shown again. The app checks for an active plan on launch.

### Main App (tab-based)

| Tab | Icon | Purpose |
|-----|------|---------|
| Home | House | Dashboard — next workout, compliance trend, race countdown |
| Plan | Calendar | Full training plan — week-by-week schedule |
| History | Clock | Completed workouts with match results |
| Profile | Person | Settings, VDOT history, goal race management |

## Screen Designs

### Home Tab (Dashboard)

Scrollable page with the following sections, top to bottom:

**Race Countdown** — Goal race name, date, and progress bar showing percentage through the plan.

**Today's Workout** — Primary card. Workout type, target distance, pace range, HR zone. Shows "Rest Day" on rest days. Flips to compliance score with checkmark if already matched.

**This Week Summary** — Week number, training phase label (Base/Quality/Peak/Taper), day-by-day indicators (completed/missed/today/upcoming) with single-letter workout type abbreviations, count of completed workouts.

**Recent Trend** — Rolling 7-day compliance percentage compared to previous period with directional indicator.

**Notification Banner** — Conditional. Appears for flagged VDOT changes, plan adjustments, or sync failures. Tapping navigates to the relevant detail.

**Sync Indicator** — Subtle "Last synced: X ago" text.

### Plan Tab

**Week Selector** — Horizontally swipeable. Shows week number, total weeks, training phase, and date range. Defaults to current week.

**Weekly Workout List** — One row per day showing:
- Completed workouts: compliance score with green/yellow/red color coding
- Today's workout: highlighted with target pace
- Upcoming workouts: target pace and distance
- Missed workouts: red indicator
- Rest days: minimal label

**Workout Detail** — Tap any workout to navigate to detail view showing:
- Planned section: distance, pace range, HR zone
- Actual section (if completed): distance, actual pace, avg HR
- Compliance breakdown: three individual bars for distance (40% weight), pace (40%), HR zone (20%)

### History Tab

**Workout List** — Reverse chronological, grouped by date. Each entry shows distance, pace, avg HR, and match status (matched with compliance score, or unmatched).

**Tapping a workout** opens the same workout detail view used by the Plan tab.

**Filter toggles** at top:
- All — every synced workout
- Matched — only workouts matched to the plan
- Flagged — workouts that triggered VDOT recalculation

### Profile Tab

**User Info** — Name, current VDOT, units, max HR. Tap to edit.

**VDOT History** — Line chart showing VDOT over time. Tapping a data point shows the triggering workout or VO2 max snapshot. This is the only chart in the app.

**Goal Race** — Active goal with edit/archive options. Past goals listed below. "New Goal Race" button reuses the wizard from onboarding.

**Settings** — Preferred units, max heart rate, log out. Minimal.

## Data Flow

### App Startup Sequence

1. Check for stored JWT token
   - No token → show Login/Register
   - Token exists but expired → call `POST /auth/refresh`
     - Refresh fails → show Login
     - Refresh succeeds → continue
2. Check for active plan (`GET /plans/active`)
   - No active plan → show Goal Race Wizard (covers both first-time users and returning users who archived their previous plan)
   - Active plan exists → continue
3. Trigger silent health sync
   - Read Apple Health data (workouts + VO2 max since last_synced_at)
   - Call `POST /api/v1/health/sync`
   - Response indicates: workouts matched, VDOT updated, adjustment made
4. Show Home tab

### Screen-to-API Mapping

| Screen | API Calls | Trigger |
|--------|-----------|---------|
| Register | `POST /auth/register` | On submit |
| Login | `POST /auth/login` | On submit |
| Profile Setup | `PATCH /users/me` | On submit |
| Health Sync | `POST /health/sync` | App open + after HealthKit permission |
| Goal Race Wizard | `POST /goal-races` then `POST /plans` | On "Generate Plan" |
| Home Tab | `GET /plans/active`, `GET /workouts?since=` | Tab load / pull-to-refresh |
| Plan Tab | `GET /plans/{id}/workouts?from=&to=` | Week change |
| History Tab | `GET /workouts` | Tab load / scroll pagination |
| Profile Tab | `GET /users/me`, `GET /vdot/history` | Tab load |
| VDOT Review | `POST /vdot/history/{id}/accept` or `/dismiss` | User tap |
| Edit Profile | `PATCH /users/me` | On save |
| Edit Goal Race | `PATCH /goal-races/{id}` | On save |

### State Management (Provider Pattern)

Providers are declared at the top of the widget tree in `main.dart` and accessible to all screens below:

- **AuthProvider** — JWT tokens, login/logout/refresh state
- **PlanProvider** — Active training plan and its workouts
- **WorkoutProvider** — Workout history
- **UserProvider** — Profile data, VDOT history
- **SyncProvider** — Health sync process and "last synced" status

When a Provider's data changes, any widget watching it automatically rebuilds.

### Error Handling

- **Network errors** — Snackbar message: "Couldn't reach server. Pull to refresh."
- **Auth errors (401)** — Automatic token refresh attempt. On failure, redirect to login.
- **Sync failures** — Update sync indicator to "Sync failed." App remains usable with previously loaded data.

No local database in V1. Network connectivity required to load data.

## Project Structure

```
run_planner_app/
├── lib/
│   ├── main.dart
│   ├── config/
│   │   └── api_config.dart
│   ├── models/
│   │   ├── user.dart
│   │   ├── goal_race.dart
│   │   ├── training_plan.dart
│   │   ├── planned_workout.dart
│   │   ├── workout.dart
│   │   ├── vdot_history.dart
│   │   └── health_sync.dart
│   ├── providers/
│   │   ├── auth_provider.dart
│   │   ├── plan_provider.dart
│   │   ├── workout_provider.dart
│   │   ├── user_provider.dart
│   │   └── sync_provider.dart
│   ├── services/
│   │   ├── api_client.dart
│   │   ├── auth_service.dart
│   │   ├── plan_service.dart
│   │   ├── workout_service.dart
│   │   ├── user_service.dart
│   │   ├── health_service.dart
│   │   └── sync_service.dart
│   ├── screens/
│   │   ├── onboarding/
│   │   │   ├── welcome_screen.dart
│   │   │   ├── profile_setup_screen.dart
│   │   │   ├── health_permission_screen.dart
│   │   │   ├── initial_sync_screen.dart
│   │   │   └── goal_race_wizard_screen.dart
│   │   ├── home/
│   │   │   └── home_screen.dart
│   │   ├── plan/
│   │   │   ├── plan_screen.dart
│   │   │   └── workout_detail_screen.dart
│   │   ├── history/
│   │   │   └── history_screen.dart
│   │   └── profile/
│   │       ├── profile_screen.dart
│   │       └── edit_profile_screen.dart
│   ├── widgets/
│   │   ├── race_countdown_card.dart
│   │   ├── workout_card.dart
│   │   ├── compliance_bars.dart
│   │   ├── week_day_indicators.dart
│   │   ├── vdot_chart.dart
│   │   ├── sync_indicator.dart
│   │   └── notification_banner.dart
│   └── theme/
│       └── app_theme.dart
├── test/
├── pubspec.yaml
├── ios/
└── android/
```

## Dependencies

| Package | Purpose |
|---------|---------|
| `provider` | State management — Provider pattern for DI across widget tree |
| `http` | HTTP client for REST API calls |
| `flutter_secure_storage` | Encrypted JWT token storage (iOS Keychain) |
| `health` | Apple HealthKit and Google Health Connect integration |
| `fl_chart` | VDOT history line chart |
| `intl` | Date/time and pace formatting |
| `go_router` | Declarative navigation and routing |

## Future Considerations (not in V1)

- Local database caching for offline support (smart client evolution)
- Android support via Google Health Connect through the `health` package
- Strava and Samsung Health as additional data sources
- Push notifications for workout reminders
