# Flutter Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Flutter iOS app that provides account creation, goal race setup, training plan viewing, and Apple Health sync — a thin client for the existing run-planner Spring Boot backend.

**Architecture:** Tab-based navigation (Home, Plan, History, Profile) with a linear onboarding flow. Provider pattern for state management. All business logic on the backend — the app displays data and syncs health information. Services handle HTTP calls, Providers hold state and notify widgets.

**Tech Stack:** Flutter 3.x, Dart, Provider, go_router, http, flutter_secure_storage, health, fl_chart, intl

---

## File Structure

```
run_planner_app/
├── lib/
│   ├── main.dart                              — App entry point, provider wiring, router
│   ├── config/
│   │   └── api_config.dart                    — Base URL, endpoint constants
│   ├── models/
│   │   ├── auth.dart                          — AuthResponse, LoginRequest, RegisterRequest, RefreshRequest
│   │   ├── user.dart                          — UserResponse, UpdateProfileRequest, Units enum
│   │   ├── goal_race.dart                     — GoalRaceResponse, CreateGoalRaceRequest, UpdateGoalRaceRequest, GoalRaceStatus enum
│   │   ├── training_plan.dart                 — TrainingPlanResponse, CreatePlanRequest, TrainingPlanStatus enum
│   │   ├── planned_workout.dart               — PlannedWorkoutResponse, WorkoutType enum, TrainingPhase enum
│   │   ├── workout.dart                       — WorkoutResponse
│   │   ├── vdot.dart                          — VdotHistoryResponse, TrainingZone enum
│   │   └── health_sync.dart                   — HealthSyncRequest, HealthSyncResponse, WorkoutSyncItem, HealthSnapshotSyncItem
│   ├── services/
│   │   ├── api_client.dart                    — HTTP client with JWT headers, token refresh, error handling
│   │   ├── auth_service.dart                  — Register, login, refresh, logout API calls
│   │   ├── user_service.dart                  — Get/update profile API calls
│   │   ├── goal_race_service.dart             — Create, list, update goal races
│   │   ├── plan_service.dart                  — Create, list, get active, get workouts
│   │   ├── workout_service.dart               — List, get workout
│   │   ├── vdot_service.dart                  — Get history, accept/dismiss flagged
│   │   ├── health_sync_service.dart           — Post health sync
│   │   └── health_kit_service.dart            — Apple HealthKit read (workouts, VO2 max, HR)
│   ├── providers/
│   │   ├── auth_provider.dart                 — JWT state, login/logout/refresh, token persistence
│   │   ├── user_provider.dart                 — User profile + VDOT history state
│   │   ├── plan_provider.dart                 — Active plan + planned workouts state
│   │   ├── workout_provider.dart              — Workout history state
│   │   └── sync_provider.dart                 — Health sync orchestration + last synced state
│   ├── screens/
│   │   ├── onboarding/
│   │   │   ├── welcome_screen.dart            — Login/register forms
│   │   │   ├── profile_setup_screen.dart      — Name, DOB, max HR, units
│   │   │   ├── health_permission_screen.dart  — HealthKit explanation + permission request
│   │   │   ├── initial_sync_screen.dart       — Loading screen during first sync
│   │   │   └── goal_race_wizard_screen.dart   — 4-step guided race + plan setup
│   │   ├── home/
│   │   │   └── home_screen.dart               — Dashboard with all widget sections
│   │   ├── plan/
│   │   │   ├── plan_screen.dart               — Week selector + workout list
│   │   │   └── workout_detail_screen.dart     — Planned vs actual comparison
│   │   ├── history/
│   │   │   └── history_screen.dart            — Chronological workout list with filters
│   │   └── profile/
│   │       ├── profile_screen.dart            — User info, VDOT chart, goal race, settings
│   │       └── edit_profile_screen.dart       — Edit name, DOB, max HR, units
│   ├── widgets/
│   │   ├── race_countdown_card.dart           — Goal race name, date, progress bar
│   │   ├── workout_card.dart                  — Single workout row (used in Plan + History)
│   │   ├── compliance_bars.dart               — Three-factor compliance breakdown bars
│   │   ├── week_day_indicators.dart           — Mo-Su row with status icons
│   │   ├── vdot_chart.dart                    — VDOT history line chart
│   │   ├── sync_indicator.dart                — "Last synced X ago" text
│   │   └── notification_banner.dart           — Dismissable alert banner
│   └── theme/
│       └── app_theme.dart                     — Colors, text styles, spacing constants
├── test/
│   ├── models/
│   │   ├── auth_test.dart
│   │   ├── user_test.dart
│   │   ├── goal_race_test.dart
│   │   ├── training_plan_test.dart
│   │   ├── planned_workout_test.dart
│   │   ├── workout_test.dart
│   │   ├── vdot_test.dart
│   │   └── health_sync_test.dart
│   ├── services/
│   │   ├── api_client_test.dart
│   │   ├── auth_service_test.dart
│   │   ├── user_service_test.dart
│   │   ├── goal_race_service_test.dart
│   │   ├── plan_service_test.dart
│   │   ├── workout_service_test.dart
│   │   ├── vdot_service_test.dart
│   │   └── health_sync_service_test.dart
│   ├── providers/
│   │   ├── auth_provider_test.dart
│   │   ├── user_provider_test.dart
│   │   ├── plan_provider_test.dart
│   │   ├── workout_provider_test.dart
│   │   └── sync_provider_test.dart
│   └── widgets/
│       ├── race_countdown_card_test.dart
│       ├── workout_card_test.dart
│       ├── compliance_bars_test.dart
│       ├── week_day_indicators_test.dart
│       └── notification_banner_test.dart
├── pubspec.yaml
├── ios/
└── android/
```

---

## Task 1: Project Scaffolding and Dependencies

**Files:**
- Create: `run_planner_app/` (Flutter project root)
- Modify: `run_planner_app/pubspec.yaml`
- Modify: `run_planner_app/lib/main.dart`

- [ ] **Step 1: Create the Flutter project**

Run: `flutter create run_planner_app`

This generates the full Flutter project structure including `lib/`, `test/`, `ios/`, `android/`, and `pubspec.yaml`. Flutter's `create` command is like `mvn archetype:generate` — it scaffolds a working starter project.

Expected: A new `run_planner_app/` directory with a working Flutter app.

- [ ] **Step 2: Verify the project builds**

Run: `cd run_planner_app && flutter pub get`

Expected: "Got dependencies!" — confirms Dart/Flutter toolchain is working.

- [ ] **Step 3: Add dependencies to pubspec.yaml**

Replace the `dependencies` and `dev_dependencies` sections in `run_planner_app/pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  provider: ^6.1.2
  http: ^1.2.1
  flutter_secure_storage: ^9.2.2
  health: ^11.0.0
  fl_chart: ^0.69.0
  intl: ^0.19.0
  go_router: ^14.2.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^4.0.0
  mockito: ^5.4.4
  build_runner: ^2.4.11
  http_mock_adapter: ^0.7.0
```

- [ ] **Step 4: Install dependencies**

Run: `flutter pub get`

Expected: "Got dependencies!" — all packages resolved successfully.

- [ ] **Step 5: Replace the generated main.dart with a minimal starter**

Replace `run_planner_app/lib/main.dart` with:

```dart
import 'package:flutter/material.dart';

void main() {
  runApp(const RunPlannerApp());
}

class RunPlannerApp extends StatelessWidget {
  const RunPlannerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Run Planner',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const Scaffold(
        body: Center(
          child: Text('Run Planner'),
        ),
      ),
    );
  }
}
```

- [ ] **Step 6: Verify it compiles**

Run: `flutter analyze`

Expected: "No issues found!"

- [ ] **Step 7: Commit**

```bash
git add run_planner_app/
git commit -m "feat(flutter): scaffold Flutter project with dependencies"
```

---

## Task 2: Theme and Configuration

**Files:**
- Create: `run_planner_app/lib/theme/app_theme.dart`
- Create: `run_planner_app/lib/config/api_config.dart`

- [ ] **Step 1: Create the app theme**

Create `run_planner_app/lib/theme/app_theme.dart`:

```dart
import 'package:flutter/material.dart';

class AppTheme {
  AppTheme._();

  // Colors — clean, minimal palette
  static const Color primary = Color(0xFF1A73E8);
  static const Color primaryLight = Color(0xFFE8F0FE);
  static const Color success = Color(0xFF34A853);
  static const Color warning = Color(0xFFFBBC04);
  static const Color error = Color(0xFFEA4335);
  static const Color textPrimary = Color(0xFF202124);
  static const Color textSecondary = Color(0xFF5F6368);
  static const Color surface = Color(0xFFFAFAFA);
  static const Color cardBackground = Colors.white;
  static const Color divider = Color(0xFFE0E0E0);

  // Spacing
  static const double spacingXs = 4.0;
  static const double spacingSm = 8.0;
  static const double spacingMd = 16.0;
  static const double spacingLg = 24.0;
  static const double spacingXl = 32.0;

  // Border radius
  static const double radiusSm = 8.0;
  static const double radiusMd = 12.0;
  static const double radiusLg = 16.0;

  static ThemeData get lightTheme {
    return ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: primary,
        surface: surface,
      ),
      useMaterial3: true,
      scaffoldBackgroundColor: surface,
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.white,
        foregroundColor: textPrimary,
        elevation: 0,
        centerTitle: true,
      ),
      cardTheme: CardTheme(
        color: cardBackground,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radiusMd),
          side: const BorderSide(color: divider, width: 0.5),
        ),
        margin: const EdgeInsets.symmetric(
          horizontal: spacingMd,
          vertical: spacingSm,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radiusSm),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: spacingMd,
          vertical: spacingSm,
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          minimumSize: const Size(double.infinity, 48),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radiusSm),
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(foregroundColor: primary),
      ),
    );
  }
}
```

- [ ] **Step 2: Create the API config**

Create `run_planner_app/lib/config/api_config.dart`:

```dart
class ApiConfig {
  ApiConfig._();

  // For local development on iOS simulator, localhost works.
  // For physical device testing, use your machine's LAN IP.
  static const String baseUrl = 'http://localhost:8080';
  static const String apiPrefix = '/api/v1';

  // Auth
  static const String register = '$apiPrefix/auth/register';
  static const String login = '$apiPrefix/auth/login';
  static const String refresh = '$apiPrefix/auth/refresh';
  static const String logout = '$apiPrefix/auth/logout';

  // User
  static const String userProfile = '$apiPrefix/users/me';

  // Goal Races
  static const String goalRaces = '$apiPrefix/goal-races';
  static String goalRace(String id) => '$apiPrefix/goal-races/$id';

  // Training Plans
  static const String plans = '$apiPrefix/plans';
  static const String activePlan = '$apiPrefix/plans/active';
  static String plan(String id) => '$apiPrefix/plans/$id';
  static String planWorkouts(String id) => '$apiPrefix/plans/$id/workouts';

  // Workouts
  static const String workouts = '$apiPrefix/workouts';
  static String workout(String id) => '$apiPrefix/workouts/$id';

  // Health Sync
  static const String healthSync = '$apiPrefix/health/sync';

  // VDOT
  static const String vdotHistory = '$apiPrefix/vdot/history';
  static String vdotAccept(String id) => '$apiPrefix/vdot/history/$id/accept';
  static String vdotDismiss(String id) => '$apiPrefix/vdot/history/$id/dismiss';
}
```

- [ ] **Step 3: Verify it compiles**

Run: `flutter analyze`

Expected: "No issues found!"

- [ ] **Step 4: Commit**

```bash
git add run_planner_app/lib/theme/ run_planner_app/lib/config/
git commit -m "feat(flutter): add app theme and API configuration"
```

---

## Task 3: Models

All model classes that mirror the backend DTOs. Dart doesn't have records like Java 17+, but it has classes with factory constructors for JSON deserialization. Each model has a `fromJson` factory (for parsing API responses) and a `toJson` method (for sending requests).

**Files:**
- Create: `run_planner_app/lib/models/auth.dart`
- Create: `run_planner_app/lib/models/user.dart`
- Create: `run_planner_app/lib/models/goal_race.dart`
- Create: `run_planner_app/lib/models/training_plan.dart`
- Create: `run_planner_app/lib/models/planned_workout.dart`
- Create: `run_planner_app/lib/models/workout.dart`
- Create: `run_planner_app/lib/models/vdot.dart`
- Create: `run_planner_app/lib/models/health_sync.dart`
- Test: `run_planner_app/test/models/auth_test.dart` (and others)

- [ ] **Step 1: Write the auth model tests**

Create `run_planner_app/test/models/auth_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/auth.dart';

void main() {
  group('AuthResponse', () {
    test('fromJson parses access and refresh tokens', () {
      final json = {
        'accessToken': 'abc.def.ghi',
        'refreshToken': 'refresh-token-123',
      };

      final response = AuthResponse.fromJson(json);

      expect(response.accessToken, 'abc.def.ghi');
      expect(response.refreshToken, 'refresh-token-123');
    });
  });

  group('RegisterRequest', () {
    test('toJson includes email, password, and name', () {
      final request = RegisterRequest(
        email: 'test@example.com',
        password: 'password123',
        name: 'Test User',
      );

      final json = request.toJson();

      expect(json['email'], 'test@example.com');
      expect(json['password'], 'password123');
      expect(json['name'], 'Test User');
    });
  });

  group('LoginRequest', () {
    test('toJson includes email and password', () {
      final request = LoginRequest(
        email: 'test@example.com',
        password: 'password123',
      );

      final json = request.toJson();

      expect(json['email'], 'test@example.com');
      expect(json['password'], 'password123');
    });
  });

  group('RefreshRequest', () {
    test('toJson includes refreshToken', () {
      final request = RefreshRequest(refreshToken: 'token-123');

      final json = request.toJson();

      expect(json['refreshToken'], 'token-123');
    });
  });
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run_planner_app && flutter test test/models/auth_test.dart`

Expected: Compilation error — `models/auth.dart` does not exist yet.

- [ ] **Step 3: Implement the auth models**

Create `run_planner_app/lib/models/auth.dart`:

```dart
class AuthResponse {
  final String accessToken;
  final String refreshToken;

  const AuthResponse({
    required this.accessToken,
    required this.refreshToken,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      accessToken: json['accessToken'] as String,
      refreshToken: json['refreshToken'] as String,
    );
  }
}

class RegisterRequest {
  final String email;
  final String password;
  final String? name;

  const RegisterRequest({
    required this.email,
    required this.password,
    this.name,
  });

  Map<String, dynamic> toJson() {
    return {
      'email': email,
      'password': password,
      if (name != null) 'name': name,
    };
  }
}

class LoginRequest {
  final String email;
  final String password;

  const LoginRequest({
    required this.email,
    required this.password,
  });

  Map<String, dynamic> toJson() {
    return {
      'email': email,
      'password': password,
    };
  }
}

class RefreshRequest {
  final String refreshToken;

  const RefreshRequest({required this.refreshToken});

  Map<String, dynamic> toJson() {
    return {'refreshToken': refreshToken};
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd run_planner_app && flutter test test/models/auth_test.dart`

Expected: All 4 tests pass.

- [ ] **Step 5: Write the user model tests**

Create `run_planner_app/test/models/user_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/user.dart';

void main() {
  group('UserResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': '550e8400-e29b-41d4-a716-446655440000',
        'email': 'runner@example.com',
        'name': 'Aaron',
        'dateOfBirth': '1990-05-15',
        'maxHr': 185,
        'preferredUnits': 'METRIC',
      };

      final user = UserResponse.fromJson(json);

      expect(user.id, '550e8400-e29b-41d4-a716-446655440000');
      expect(user.email, 'runner@example.com');
      expect(user.name, 'Aaron');
      expect(user.dateOfBirth, DateTime(1990, 5, 15));
      expect(user.maxHr, 185);
      expect(user.preferredUnits, Units.metric);
    });

    test('fromJson handles null optional fields', () {
      final json = {
        'id': '550e8400-e29b-41d4-a716-446655440000',
        'email': 'runner@example.com',
        'name': null,
        'dateOfBirth': null,
        'maxHr': null,
        'preferredUnits': 'METRIC',
      };

      final user = UserResponse.fromJson(json);

      expect(user.name, isNull);
      expect(user.dateOfBirth, isNull);
      expect(user.maxHr, isNull);
    });
  });

  group('UpdateProfileRequest', () {
    test('toJson includes all fields', () {
      final request = UpdateProfileRequest(
        name: 'Aaron',
        dateOfBirth: DateTime(1990, 5, 15),
        maxHr: 185,
        preferredUnits: Units.metric,
      );

      final json = request.toJson();

      expect(json['name'], 'Aaron');
      expect(json['dateOfBirth'], '1990-05-15');
      expect(json['maxHr'], 185);
      expect(json['preferredUnits'], 'METRIC');
    });

    test('toJson omits null fields', () {
      final request = UpdateProfileRequest(name: 'Aaron');

      final json = request.toJson();

      expect(json.containsKey('name'), true);
      expect(json.containsKey('dateOfBirth'), false);
      expect(json.containsKey('maxHr'), false);
      expect(json.containsKey('preferredUnits'), false);
    });
  });

  group('Units', () {
    test('fromJson parses METRIC', () {
      expect(Units.fromJson('METRIC'), Units.metric);
    });

    test('fromJson parses IMPERIAL', () {
      expect(Units.fromJson('IMPERIAL'), Units.imperial);
    });

    test('toJson returns uppercase string', () {
      expect(Units.metric.toJson(), 'METRIC');
      expect(Units.imperial.toJson(), 'IMPERIAL');
    });
  });
}
```

- [ ] **Step 6: Implement the user models**

Create `run_planner_app/lib/models/user.dart`:

```dart
enum Units {
  metric,
  imperial;

  static Units fromJson(String value) {
    return switch (value) {
      'METRIC' => Units.metric,
      'IMPERIAL' => Units.imperial,
      _ => throw ArgumentError('Unknown Units value: $value'),
    };
  }

  String toJson() => name.toUpperCase();
}

class UserResponse {
  final String id;
  final String email;
  final String? name;
  final DateTime? dateOfBirth;
  final int? maxHr;
  final Units preferredUnits;

  const UserResponse({
    required this.id,
    required this.email,
    this.name,
    this.dateOfBirth,
    this.maxHr,
    required this.preferredUnits,
  });

  factory UserResponse.fromJson(Map<String, dynamic> json) {
    return UserResponse(
      id: json['id'] as String,
      email: json['email'] as String,
      name: json['name'] as String?,
      dateOfBirth: json['dateOfBirth'] != null
          ? DateTime.parse(json['dateOfBirth'] as String)
          : null,
      maxHr: json['maxHr'] as int?,
      preferredUnits: Units.fromJson(json['preferredUnits'] as String),
    );
  }
}

class UpdateProfileRequest {
  final String? name;
  final DateTime? dateOfBirth;
  final int? maxHr;
  final Units? preferredUnits;

  const UpdateProfileRequest({
    this.name,
    this.dateOfBirth,
    this.maxHr,
    this.preferredUnits,
  });

  Map<String, dynamic> toJson() {
    return {
      if (name != null) 'name': name,
      if (dateOfBirth != null)
        'dateOfBirth':
            '${dateOfBirth!.year}-${dateOfBirth!.month.toString().padLeft(2, '0')}-${dateOfBirth!.day.toString().padLeft(2, '0')}',
      if (maxHr != null) 'maxHr': maxHr,
      if (preferredUnits != null) 'preferredUnits': preferredUnits!.toJson(),
    };
  }
}
```

- [ ] **Step 7: Run user model tests**

Run: `cd run_planner_app && flutter test test/models/user_test.dart`

Expected: All 6 tests pass.

- [ ] **Step 8: Write the goal race model tests**

Create `run_planner_app/test/models/goal_race_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/goal_race.dart';

void main() {
  group('GoalRaceResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'race-uuid-123',
        'distanceMeters': 42195,
        'distanceLabel': 'Marathon',
        'raceDate': '2026-10-12',
        'goalFinishSeconds': 11700,
        'status': 'ACTIVE',
      };

      final race = GoalRaceResponse.fromJson(json);

      expect(race.id, 'race-uuid-123');
      expect(race.distanceMeters, 42195);
      expect(race.distanceLabel, 'Marathon');
      expect(race.raceDate, DateTime(2026, 10, 12));
      expect(race.goalFinishSeconds, 11700);
      expect(race.status, GoalRaceStatus.active);
    });

    test('fromJson handles null goalFinishSeconds', () {
      final json = {
        'id': 'race-uuid-123',
        'distanceMeters': 42195,
        'distanceLabel': 'Marathon',
        'raceDate': '2026-10-12',
        'goalFinishSeconds': null,
        'status': 'ACTIVE',
      };

      final race = GoalRaceResponse.fromJson(json);

      expect(race.goalFinishSeconds, isNull);
    });
  });

  group('CreateGoalRaceRequest', () {
    test('toJson includes all fields', () {
      final request = CreateGoalRaceRequest(
        distanceMeters: 42195,
        distanceLabel: 'Marathon',
        raceDate: DateTime(2026, 10, 12),
        goalFinishSeconds: 11700,
      );

      final json = request.toJson();

      expect(json['distanceMeters'], 42195);
      expect(json['distanceLabel'], 'Marathon');
      expect(json['raceDate'], '2026-10-12');
      expect(json['goalFinishSeconds'], 11700);
    });
  });

  group('GoalRaceStatus', () {
    test('fromJson parses all values', () {
      expect(GoalRaceStatus.fromJson('ACTIVE'), GoalRaceStatus.active);
      expect(GoalRaceStatus.fromJson('COMPLETED'), GoalRaceStatus.completed);
      expect(GoalRaceStatus.fromJson('ARCHIVED'), GoalRaceStatus.archived);
    });
  });
}
```

- [ ] **Step 9: Implement the goal race models**

Create `run_planner_app/lib/models/goal_race.dart`:

```dart
enum GoalRaceStatus {
  active,
  completed,
  archived;

  static GoalRaceStatus fromJson(String value) {
    return switch (value) {
      'ACTIVE' => GoalRaceStatus.active,
      'COMPLETED' => GoalRaceStatus.completed,
      'ARCHIVED' => GoalRaceStatus.archived,
      _ => throw ArgumentError('Unknown GoalRaceStatus: $value'),
    };
  }

  String toJson() => name.toUpperCase();
}

class GoalRaceResponse {
  final String id;
  final int distanceMeters;
  final String distanceLabel;
  final DateTime raceDate;
  final int? goalFinishSeconds;
  final GoalRaceStatus status;

  const GoalRaceResponse({
    required this.id,
    required this.distanceMeters,
    required this.distanceLabel,
    required this.raceDate,
    this.goalFinishSeconds,
    required this.status,
  });

  factory GoalRaceResponse.fromJson(Map<String, dynamic> json) {
    return GoalRaceResponse(
      id: json['id'] as String,
      distanceMeters: json['distanceMeters'] as int,
      distanceLabel: json['distanceLabel'] as String,
      raceDate: DateTime.parse(json['raceDate'] as String),
      goalFinishSeconds: json['goalFinishSeconds'] as int?,
      status: GoalRaceStatus.fromJson(json['status'] as String),
    );
  }
}

class CreateGoalRaceRequest {
  final int distanceMeters;
  final String distanceLabel;
  final DateTime raceDate;
  final int? goalFinishSeconds;

  const CreateGoalRaceRequest({
    required this.distanceMeters,
    required this.distanceLabel,
    required this.raceDate,
    this.goalFinishSeconds,
  });

  Map<String, dynamic> toJson() {
    return {
      'distanceMeters': distanceMeters,
      'distanceLabel': distanceLabel,
      'raceDate':
          '${raceDate.year}-${raceDate.month.toString().padLeft(2, '0')}-${raceDate.day.toString().padLeft(2, '0')}',
      if (goalFinishSeconds != null) 'goalFinishSeconds': goalFinishSeconds,
    };
  }
}

class UpdateGoalRaceRequest {
  final DateTime? raceDate;
  final int? goalFinishSeconds;
  final GoalRaceStatus? status;

  const UpdateGoalRaceRequest({
    this.raceDate,
    this.goalFinishSeconds,
    this.status,
  });

  Map<String, dynamic> toJson() {
    return {
      if (raceDate != null)
        'raceDate':
            '${raceDate!.year}-${raceDate!.month.toString().padLeft(2, '0')}-${raceDate!.day.toString().padLeft(2, '0')}',
      if (goalFinishSeconds != null) 'goalFinishSeconds': goalFinishSeconds,
      if (status != null) 'status': status!.toJson(),
    };
  }
}
```

- [ ] **Step 10: Run goal race model tests**

Run: `cd run_planner_app && flutter test test/models/goal_race_test.dart`

Expected: All 4 tests pass.

- [ ] **Step 11: Write the training plan and planned workout model tests**

Create `run_planner_app/test/models/training_plan_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/training_plan.dart';
import 'package:run_planner_app/models/planned_workout.dart';

void main() {
  group('TrainingPlanResponse', () {
    test('fromJson parses plan with workouts', () {
      final json = {
        'id': 'plan-uuid',
        'goalRaceId': 'race-uuid',
        'startDate': '2026-06-01',
        'endDate': '2026-10-11',
        'status': 'ACTIVE',
        'revision': 1,
        'createdAt': '2026-06-01T10:00:00Z',
        'workouts': [
          {
            'id': 'w-uuid',
            'weekNumber': 1,
            'dayOfWeek': 1,
            'scheduledDate': '2026-06-01',
            'workoutType': 'EASY',
            'targetDistanceMeters': 6000.0,
            'targetPaceMinPerKm': 5.5,
            'targetPaceMaxPerKm': 6.0,
            'targetHrZone': 140,
            'notes': null,
            'planRevision': 1,
          },
        ],
      };

      final plan = TrainingPlanResponse.fromJson(json);

      expect(plan.id, 'plan-uuid');
      expect(plan.goalRaceId, 'race-uuid');
      expect(plan.startDate, DateTime(2026, 6, 1));
      expect(plan.endDate, DateTime(2026, 10, 11));
      expect(plan.status, TrainingPlanStatus.active);
      expect(plan.revision, 1);
      expect(plan.workouts, hasLength(1));
      expect(plan.workouts.first.workoutType, WorkoutType.easy);
    });

    test('fromJson handles empty workouts list', () {
      final json = {
        'id': 'plan-uuid',
        'goalRaceId': 'race-uuid',
        'startDate': '2026-06-01',
        'endDate': '2026-10-11',
        'status': 'ACTIVE',
        'revision': 1,
        'createdAt': '2026-06-01T10:00:00Z',
        'workouts': <Map<String, dynamic>>[],
      };

      final plan = TrainingPlanResponse.fromJson(json);

      expect(plan.workouts, isEmpty);
    });

    test('fromJson handles null workouts', () {
      final json = {
        'id': 'plan-uuid',
        'goalRaceId': 'race-uuid',
        'startDate': '2026-06-01',
        'endDate': '2026-10-11',
        'status': 'ACTIVE',
        'revision': 1,
        'createdAt': '2026-06-01T10:00:00Z',
        'workouts': null,
      };

      final plan = TrainingPlanResponse.fromJson(json);

      expect(plan.workouts, isEmpty);
    });
  });
}
```

Create `run_planner_app/test/models/planned_workout_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/planned_workout.dart';

void main() {
  group('PlannedWorkoutResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'pw-uuid',
        'weekNumber': 3,
        'dayOfWeek': 5,
        'scheduledDate': '2026-06-20',
        'workoutType': 'THRESHOLD',
        'targetDistanceMeters': 8000.0,
        'targetPaceMinPerKm': 4.75,
        'targetPaceMaxPerKm': 4.92,
        'targetHrZone': 165,
        'notes': 'Steady effort',
        'planRevision': 2,
      };

      final workout = PlannedWorkoutResponse.fromJson(json);

      expect(workout.id, 'pw-uuid');
      expect(workout.weekNumber, 3);
      expect(workout.dayOfWeek, 5);
      expect(workout.scheduledDate, DateTime(2026, 6, 20));
      expect(workout.workoutType, WorkoutType.threshold);
      expect(workout.targetDistanceMeters, 8000.0);
      expect(workout.targetPaceMinPerKm, 4.75);
      expect(workout.targetPaceMaxPerKm, 4.92);
      expect(workout.targetHrZone, 165);
      expect(workout.notes, 'Steady effort');
      expect(workout.planRevision, 2);
    });

    test('fromJson handles REST day', () {
      final json = {
        'id': 'pw-rest',
        'weekNumber': 1,
        'dayOfWeek': 7,
        'scheduledDate': '2026-06-07',
        'workoutType': 'REST',
        'targetDistanceMeters': 0.0,
        'targetPaceMinPerKm': null,
        'targetPaceMaxPerKm': null,
        'targetHrZone': null,
        'notes': null,
        'planRevision': 1,
      };

      final workout = PlannedWorkoutResponse.fromJson(json);

      expect(workout.workoutType, WorkoutType.rest);
      expect(workout.targetPaceMinPerKm, isNull);
    });
  });

  group('WorkoutType', () {
    test('abbreviation returns single letter', () {
      expect(WorkoutType.easy.abbreviation, 'E');
      expect(WorkoutType.long_.abbreviation, 'L');
      expect(WorkoutType.marathon.abbreviation, 'M');
      expect(WorkoutType.threshold.abbreviation, 'T');
      expect(WorkoutType.interval.abbreviation, 'I');
      expect(WorkoutType.repetition.abbreviation, 'R');
      expect(WorkoutType.rest.abbreviation, '-');
    });

    test('displayName returns human readable name', () {
      expect(WorkoutType.easy.displayName, 'Easy Run');
      expect(WorkoutType.long_.displayName, 'Long Run');
      expect(WorkoutType.threshold.displayName, 'Threshold Run');
      expect(WorkoutType.rest.displayName, 'Rest Day');
    });
  });

  group('TrainingPhase', () {
    test('fromJson parses all values', () {
      expect(TrainingPhase.fromJson('BASE'), TrainingPhase.base);
      expect(TrainingPhase.fromJson('QUALITY'), TrainingPhase.quality);
      expect(TrainingPhase.fromJson('PEAK'), TrainingPhase.peak);
      expect(TrainingPhase.fromJson('TAPER'), TrainingPhase.taper);
    });
  });
}
```

- [ ] **Step 12: Implement the training plan and planned workout models**

Create `run_planner_app/lib/models/planned_workout.dart`:

```dart
enum WorkoutType {
  easy('Easy Run', 'E'),
  long_('Long Run', 'L'),
  marathon('Marathon Pace', 'M'),
  threshold('Threshold Run', 'T'),
  interval('Intervals', 'I'),
  repetition('Repetitions', 'R'),
  rest('Rest Day', '-');

  final String displayName;
  final String abbreviation;

  const WorkoutType(this.displayName, this.abbreviation);

  static WorkoutType fromJson(String value) {
    return switch (value) {
      'EASY' => WorkoutType.easy,
      'LONG' => WorkoutType.long_,
      'MARATHON' => WorkoutType.marathon,
      'THRESHOLD' => WorkoutType.threshold,
      'INTERVAL' => WorkoutType.interval,
      'REPETITION' => WorkoutType.repetition,
      'REST' => WorkoutType.rest,
      _ => throw ArgumentError('Unknown WorkoutType: $value'),
    };
  }

  String toJson() {
    return switch (this) {
      WorkoutType.long_ => 'LONG',
      _ => name.toUpperCase(),
    };
  }
}

enum TrainingPhase {
  base,
  quality,
  peak,
  taper;

  static TrainingPhase fromJson(String value) {
    return switch (value) {
      'BASE' => TrainingPhase.base,
      'QUALITY' => TrainingPhase.quality,
      'PEAK' => TrainingPhase.peak,
      'TAPER' => TrainingPhase.taper,
      _ => throw ArgumentError('Unknown TrainingPhase: $value'),
    };
  }

  String get displayName => '${name[0].toUpperCase()}${name.substring(1)}';
}

class PlannedWorkoutResponse {
  final String id;
  final int weekNumber;
  final int dayOfWeek;
  final DateTime scheduledDate;
  final WorkoutType workoutType;
  final double targetDistanceMeters;
  final double? targetPaceMinPerKm;
  final double? targetPaceMaxPerKm;
  final int? targetHrZone;
  final String? notes;
  final int planRevision;

  const PlannedWorkoutResponse({
    required this.id,
    required this.weekNumber,
    required this.dayOfWeek,
    required this.scheduledDate,
    required this.workoutType,
    required this.targetDistanceMeters,
    this.targetPaceMinPerKm,
    this.targetPaceMaxPerKm,
    this.targetHrZone,
    this.notes,
    required this.planRevision,
  });

  factory PlannedWorkoutResponse.fromJson(Map<String, dynamic> json) {
    return PlannedWorkoutResponse(
      id: json['id'] as String,
      weekNumber: json['weekNumber'] as int,
      dayOfWeek: json['dayOfWeek'] as int,
      scheduledDate: DateTime.parse(json['scheduledDate'] as String),
      workoutType: WorkoutType.fromJson(json['workoutType'] as String),
      targetDistanceMeters: (json['targetDistanceMeters'] as num).toDouble(),
      targetPaceMinPerKm: (json['targetPaceMinPerKm'] as num?)?.toDouble(),
      targetPaceMaxPerKm: (json['targetPaceMaxPerKm'] as num?)?.toDouble(),
      targetHrZone: json['targetHrZone'] as int?,
      notes: json['notes'] as String?,
      planRevision: json['planRevision'] as int,
    );
  }
}
```

Create `run_planner_app/lib/models/training_plan.dart`:

```dart
import 'planned_workout.dart';

enum TrainingPlanStatus {
  active,
  completed,
  archived;

  static TrainingPlanStatus fromJson(String value) {
    return switch (value) {
      'ACTIVE' => TrainingPlanStatus.active,
      'COMPLETED' => TrainingPlanStatus.completed,
      'ARCHIVED' => TrainingPlanStatus.archived,
      _ => throw ArgumentError('Unknown TrainingPlanStatus: $value'),
    };
  }
}

class TrainingPlanResponse {
  final String id;
  final String goalRaceId;
  final DateTime startDate;
  final DateTime endDate;
  final TrainingPlanStatus status;
  final int revision;
  final DateTime createdAt;
  final List<PlannedWorkoutResponse> workouts;

  const TrainingPlanResponse({
    required this.id,
    required this.goalRaceId,
    required this.startDate,
    required this.endDate,
    required this.status,
    required this.revision,
    required this.createdAt,
    required this.workouts,
  });

  factory TrainingPlanResponse.fromJson(Map<String, dynamic> json) {
    return TrainingPlanResponse(
      id: json['id'] as String,
      goalRaceId: json['goalRaceId'] as String,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      status: TrainingPlanStatus.fromJson(json['status'] as String),
      revision: json['revision'] as int,
      createdAt: DateTime.parse(json['createdAt'] as String),
      workouts: (json['workouts'] as List<dynamic>?)
              ?.map((w) =>
                  PlannedWorkoutResponse.fromJson(w as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}

class CreatePlanRequest {
  final String goalRaceId;

  const CreatePlanRequest({required this.goalRaceId});

  Map<String, dynamic> toJson() {
    return {'goalRaceId': goalRaceId};
  }
}
```

- [ ] **Step 13: Run training plan and planned workout tests**

Run: `cd run_planner_app && flutter test test/models/training_plan_test.dart test/models/planned_workout_test.dart`

Expected: All tests pass.

- [ ] **Step 14: Write and implement the remaining models (workout, vdot, health_sync)**

Create `run_planner_app/test/models/workout_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/workout.dart';

void main() {
  group('WorkoutResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'workout-uuid',
        'startedAt': '2026-03-25T08:00:00Z',
        'distanceMeters': 10000.0,
        'durationSeconds': 3000,
        'avgHr': 155,
        'maxHr': 172,
        'elevationGain': 45.0,
        'source': 'APPLE_HEALTH',
        'sourceId': 'ah-123',
      };

      final workout = WorkoutResponse.fromJson(json);

      expect(workout.id, 'workout-uuid');
      expect(workout.startedAt, DateTime.utc(2026, 3, 25, 8));
      expect(workout.distanceMeters, 10000.0);
      expect(workout.durationSeconds, 3000);
      expect(workout.avgHr, 155);
      expect(workout.maxHr, 172);
      expect(workout.elevationGain, 45.0);
      expect(workout.source, 'APPLE_HEALTH');
      expect(workout.sourceId, 'ah-123');
    });

    test('fromJson handles null optional fields', () {
      final json = {
        'id': 'workout-uuid',
        'startedAt': '2026-03-25T08:00:00Z',
        'distanceMeters': 5000.0,
        'durationSeconds': 1500,
        'avgHr': null,
        'maxHr': null,
        'elevationGain': null,
        'source': 'APPLE_HEALTH',
        'sourceId': 'ah-456',
      };

      final workout = WorkoutResponse.fromJson(json);

      expect(workout.avgHr, isNull);
      expect(workout.maxHr, isNull);
      expect(workout.elevationGain, isNull);
    });

    test('paceMinPerKm calculates correctly', () {
      final json = {
        'id': 'w-uuid',
        'startedAt': '2026-03-25T08:00:00Z',
        'distanceMeters': 10000.0,
        'durationSeconds': 3000,
        'avgHr': 155,
        'maxHr': 172,
        'elevationGain': null,
        'source': 'APPLE_HEALTH',
        'sourceId': 'ah-789',
      };

      final workout = WorkoutResponse.fromJson(json);

      // 3000 seconds / (10000m / 1000) = 300 sec/km = 5.0 min/km
      expect(workout.paceMinPerKm, 5.0);
    });
  });
}
```

Create `run_planner_app/lib/models/workout.dart`:

```dart
class WorkoutResponse {
  final String id;
  final DateTime startedAt;
  final double distanceMeters;
  final int durationSeconds;
  final int? avgHr;
  final int? maxHr;
  final double? elevationGain;
  final String source;
  final String sourceId;

  const WorkoutResponse({
    required this.id,
    required this.startedAt,
    required this.distanceMeters,
    required this.durationSeconds,
    this.avgHr,
    this.maxHr,
    this.elevationGain,
    required this.source,
    required this.sourceId,
  });

  double get paceMinPerKm {
    if (distanceMeters <= 0) return 0;
    return (durationSeconds / 60) / (distanceMeters / 1000);
  }

  factory WorkoutResponse.fromJson(Map<String, dynamic> json) {
    return WorkoutResponse(
      id: json['id'] as String,
      startedAt: DateTime.parse(json['startedAt'] as String),
      distanceMeters: (json['distanceMeters'] as num).toDouble(),
      durationSeconds: json['durationSeconds'] as int,
      avgHr: json['avgHr'] as int?,
      maxHr: json['maxHr'] as int?,
      elevationGain: (json['elevationGain'] as num?)?.toDouble(),
      source: json['source'] as String,
      sourceId: json['sourceId'] as String,
    );
  }
}
```

Create `run_planner_app/test/models/vdot_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/vdot.dart';

void main() {
  group('VdotHistoryResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'vdot-uuid',
        'triggeringWorkoutId': 'workout-uuid',
        'triggeringSnapshotId': null,
        'previousVdot': 45.0,
        'newVdot': 48.2,
        'calculatedAt': '2026-03-25T08:30:00Z',
        'flagged': false,
        'accepted': true,
      };

      final entry = VdotHistoryResponse.fromJson(json);

      expect(entry.id, 'vdot-uuid');
      expect(entry.triggeringWorkoutId, 'workout-uuid');
      expect(entry.triggeringSnapshotId, isNull);
      expect(entry.previousVdot, 45.0);
      expect(entry.newVdot, 48.2);
      expect(entry.flagged, false);
      expect(entry.accepted, true);
    });
  });

  group('TrainingZone', () {
    test('fromJson parses all zones', () {
      expect(TrainingZone.fromJson('E'), TrainingZone.easy);
      expect(TrainingZone.fromJson('M'), TrainingZone.marathon);
      expect(TrainingZone.fromJson('T'), TrainingZone.threshold);
      expect(TrainingZone.fromJson('I'), TrainingZone.interval);
      expect(TrainingZone.fromJson('R'), TrainingZone.repetition);
    });
  });
}
```

Create `run_planner_app/lib/models/vdot.dart`:

```dart
enum TrainingZone {
  easy('Easy', 'E'),
  marathon('Marathon', 'M'),
  threshold('Threshold', 'T'),
  interval('Interval', 'I'),
  repetition('Repetition', 'R');

  final String displayName;
  final String abbreviation;

  const TrainingZone(this.displayName, this.abbreviation);

  static TrainingZone fromJson(String value) {
    return switch (value) {
      'E' => TrainingZone.easy,
      'M' => TrainingZone.marathon,
      'T' => TrainingZone.threshold,
      'I' => TrainingZone.interval,
      'R' => TrainingZone.repetition,
      _ => throw ArgumentError('Unknown TrainingZone: $value'),
    };
  }
}

class VdotHistoryResponse {
  final String id;
  final String? triggeringWorkoutId;
  final String? triggeringSnapshotId;
  final double previousVdot;
  final double newVdot;
  final DateTime calculatedAt;
  final bool flagged;
  final bool accepted;

  const VdotHistoryResponse({
    required this.id,
    this.triggeringWorkoutId,
    this.triggeringSnapshotId,
    required this.previousVdot,
    required this.newVdot,
    required this.calculatedAt,
    required this.flagged,
    required this.accepted,
  });

  factory VdotHistoryResponse.fromJson(Map<String, dynamic> json) {
    return VdotHistoryResponse(
      id: json['id'] as String,
      triggeringWorkoutId: json['triggeringWorkoutId'] as String?,
      triggeringSnapshotId: json['triggeringSnapshotId'] as String?,
      previousVdot: (json['previousVdot'] as num).toDouble(),
      newVdot: (json['newVdot'] as num).toDouble(),
      calculatedAt: DateTime.parse(json['calculatedAt'] as String),
      flagged: json['flagged'] as bool,
      accepted: json['accepted'] as bool,
    );
  }
}
```

Create `run_planner_app/test/models/health_sync_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/health_sync.dart';

void main() {
  group('HealthSyncRequest', () {
    test('toJson serializes workouts and snapshots', () {
      final request = HealthSyncRequest(
        workouts: [
          WorkoutSyncItem(
            source: 'APPLE_HEALTH',
            sourceId: 'ah-123',
            startedAt: DateTime.utc(2026, 3, 25, 8),
            distanceMeters: 10000.0,
            durationSeconds: 3000,
            avgHr: 155,
            maxHr: 172,
            elevationGain: 45.0,
          ),
        ],
        healthSnapshots: [
          HealthSnapshotSyncItem(
            vo2maxEstimate: 48.5,
            restingHr: 52,
            recordedAt: DateTime.utc(2026, 3, 25, 6),
          ),
        ],
      );

      final json = request.toJson();

      expect(json['workouts'], hasLength(1));
      expect(json['workouts'][0]['source'], 'APPLE_HEALTH');
      expect(json['workouts'][0]['distanceMeters'], 10000.0);
      expect(json['healthSnapshots'], hasLength(1));
      expect(json['healthSnapshots'][0]['vo2maxEstimate'], 48.5);
    });
  });

  group('HealthSyncResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'workoutsSaved': 3,
        'workoutsSkipped': 1,
        'workoutsMatched': 2,
        'snapshotsSaved': 1,
        'vdotUpdated': true,
        'adjustmentApplied': 'NONE',
      };

      final response = HealthSyncResponse.fromJson(json);

      expect(response.workoutsSaved, 3);
      expect(response.workoutsSkipped, 1);
      expect(response.workoutsMatched, 2);
      expect(response.snapshotsSaved, 1);
      expect(response.vdotUpdated, true);
      expect(response.adjustmentApplied, 'NONE');
    });
  });
}
```

Create `run_planner_app/lib/models/health_sync.dart`:

```dart
class WorkoutSyncItem {
  final String source;
  final String? sourceId;
  final DateTime startedAt;
  final double distanceMeters;
  final int durationSeconds;
  final int? avgHr;
  final int? maxHr;
  final double? elevationGain;

  const WorkoutSyncItem({
    required this.source,
    this.sourceId,
    required this.startedAt,
    required this.distanceMeters,
    required this.durationSeconds,
    this.avgHr,
    this.maxHr,
    this.elevationGain,
  });

  Map<String, dynamic> toJson() {
    return {
      'source': source,
      if (sourceId != null) 'sourceId': sourceId,
      'startedAt': startedAt.toUtc().toIso8601String(),
      'distanceMeters': distanceMeters,
      'durationSeconds': durationSeconds,
      if (avgHr != null) 'avgHr': avgHr,
      if (maxHr != null) 'maxHr': maxHr,
      if (elevationGain != null) 'elevationGain': elevationGain,
    };
  }
}

class HealthSnapshotSyncItem {
  final double? vo2maxEstimate;
  final int? restingHr;
  final DateTime recordedAt;

  const HealthSnapshotSyncItem({
    this.vo2maxEstimate,
    this.restingHr,
    required this.recordedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      if (vo2maxEstimate != null) 'vo2maxEstimate': vo2maxEstimate,
      if (restingHr != null) 'restingHr': restingHr,
      'recordedAt': recordedAt.toUtc().toIso8601String(),
    };
  }
}

class HealthSyncRequest {
  final List<WorkoutSyncItem> workouts;
  final List<HealthSnapshotSyncItem> healthSnapshots;

  const HealthSyncRequest({
    required this.workouts,
    required this.healthSnapshots,
  });

  Map<String, dynamic> toJson() {
    return {
      'workouts': workouts.map((w) => w.toJson()).toList(),
      'healthSnapshots': healthSnapshots.map((s) => s.toJson()).toList(),
    };
  }
}

class HealthSyncResponse {
  final int workoutsSaved;
  final int workoutsSkipped;
  final int workoutsMatched;
  final int snapshotsSaved;
  final bool vdotUpdated;
  final String adjustmentApplied;

  const HealthSyncResponse({
    required this.workoutsSaved,
    required this.workoutsSkipped,
    required this.workoutsMatched,
    required this.snapshotsSaved,
    required this.vdotUpdated,
    required this.adjustmentApplied,
  });

  factory HealthSyncResponse.fromJson(Map<String, dynamic> json) {
    return HealthSyncResponse(
      workoutsSaved: json['workoutsSaved'] as int,
      workoutsSkipped: json['workoutsSkipped'] as int,
      workoutsMatched: json['workoutsMatched'] as int,
      snapshotsSaved: json['snapshotsSaved'] as int,
      vdotUpdated: json['vdotUpdated'] as bool,
      adjustmentApplied: json['adjustmentApplied'] as String,
    );
  }
}
```

- [ ] **Step 15: Run all model tests**

Run: `cd run_planner_app && flutter test test/models/`

Expected: All tests pass.

- [ ] **Step 16: Commit**

```bash
git add run_planner_app/lib/models/ run_planner_app/test/models/
git commit -m "feat(flutter): add all model classes mirroring backend DTOs"
```

---

## Task 4: API Client

The HTTP client that handles JWT headers, automatic token refresh on 401, and JSON serialization. This is the foundation every service builds on.

**Files:**
- Create: `run_planner_app/lib/services/api_client.dart`
- Create: `run_planner_app/test/services/api_client_test.dart`

- [ ] **Step 1: Write the API client tests**

Create `run_planner_app/test/services/api_client_test.dart`:

```dart
import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/config/api_config.dart';

void main() {
  group('ApiClient', () {
    test('get includes auth header when token is set', () async {
      String? capturedAuthHeader;

      final mockClient = MockClient((request) async {
        capturedAuthHeader = request.headers['Authorization'];
        return http.Response('{"ok": true}', 200);
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => 'test-jwt-token',
      );

      await apiClient.get(ApiConfig.userProfile);

      expect(capturedAuthHeader, 'Bearer test-jwt-token');
    });

    test('get omits auth header when token is null', () async {
      String? capturedAuthHeader;

      final mockClient = MockClient((request) async {
        capturedAuthHeader = request.headers['Authorization'];
        return http.Response('{"ok": true}', 200);
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => null,
      );

      await apiClient.get(ApiConfig.userProfile);

      expect(capturedAuthHeader, isNull);
    });

    test('post sends JSON body', () async {
      String? capturedBody;
      String? capturedContentType;

      final mockClient = MockClient((request) async {
        capturedBody = request.body;
        capturedContentType = request.headers['Content-Type'];
        return http.Response('{"id": "123"}', 200);
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => 'token',
      );

      await apiClient.post(
        ApiConfig.register,
        body: {'email': 'test@example.com', 'password': 'pass123'},
      );

      expect(capturedContentType, 'application/json');
      final decoded = jsonDecode(capturedBody!);
      expect(decoded['email'], 'test@example.com');
    });

    test('throws ApiException on non-2xx response', () async {
      final mockClient = MockClient((request) async {
        return http.Response('{"message": "Not found"}', 404);
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => 'token',
      );

      expect(
        () => apiClient.get('/api/v1/nonexistent'),
        throwsA(isA<ApiException>()
            .having((e) => e.statusCode, 'statusCode', 404)),
      );
    });
  });
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run_planner_app && flutter test test/services/api_client_test.dart`

Expected: Compilation error — `services/api_client.dart` does not exist.

- [ ] **Step 3: Implement the API client**

Create `run_planner_app/lib/services/api_client.dart`:

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  const ApiException({required this.statusCode, required this.message});

  @override
  String toString() => 'ApiException($statusCode): $message';
}

class ApiClient {
  final http.Client _httpClient;
  final String? Function() _getToken;
  final Future<void> Function()? onUnauthorized;

  ApiClient({
    http.Client? httpClient,
    required String? Function() getToken,
    this.onUnauthorized,
  })  : _httpClient = httpClient ?? http.Client(),
        _getToken = getToken;

  Map<String, String> _headers() {
    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    final token = _getToken();
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  Future<Map<String, dynamic>> get(String path,
      {Map<String, String>? queryParams}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path')
        .replace(queryParameters: queryParams);
    final response = await _httpClient.get(uri, headers: _headers());
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> post(String path,
      {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final response = await _httpClient.post(
      uri,
      headers: _headers(),
      body: body != null ? jsonEncode(body) : null,
    );
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> patch(String path,
      {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final response = await _httpClient.patch(
      uri,
      headers: _headers(),
      body: body != null ? jsonEncode(body) : null,
    );
    return _handleResponse(response);
  }

  Future<void> delete(String path) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final response = await _httpClient.delete(uri, headers: _headers());
    if (response.statusCode == 401 && onUnauthorized != null) {
      await onUnauthorized!();
    }
    if (response.statusCode >= 300) {
      throw ApiException(
        statusCode: response.statusCode,
        message: response.body,
      );
    }
  }

  Future<List<dynamic>> getList(String path,
      {Map<String, String>? queryParams}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path')
        .replace(queryParameters: queryParams);
    final response = await _httpClient.get(uri, headers: _headers());
    if (response.statusCode == 401 && onUnauthorized != null) {
      await onUnauthorized!();
    }
    if (response.statusCode >= 300) {
      throw ApiException(
        statusCode: response.statusCode,
        message: response.body,
      );
    }
    return jsonDecode(response.body) as List<dynamic>;
  }

  Map<String, dynamic> _handleResponse(http.Response response) {
    if (response.statusCode == 401 && onUnauthorized != null) {
      onUnauthorized!();
    }
    if (response.statusCode >= 300) {
      throw ApiException(
        statusCode: response.statusCode,
        message: response.body,
      );
    }
    if (response.body.isEmpty) return {};
    return jsonDecode(response.body) as Map<String, dynamic>;
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd run_planner_app && flutter test test/services/api_client_test.dart`

Expected: All 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add run_planner_app/lib/services/api_client.dart run_planner_app/test/services/api_client_test.dart
git commit -m "feat(flutter): add API client with JWT auth and error handling"
```

---

## Task 5: Auth Service and Provider

Handles registration, login, token persistence, and token refresh.

**Files:**
- Create: `run_planner_app/lib/services/auth_service.dart`
- Create: `run_planner_app/lib/providers/auth_provider.dart`
- Create: `run_planner_app/test/services/auth_service_test.dart`
- Create: `run_planner_app/test/providers/auth_provider_test.dart`

- [ ] **Step 1: Write the auth service tests**

Create `run_planner_app/test/services/auth_service_test.dart`:

```dart
import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/models/auth.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/auth_service.dart';

void main() {
  group('AuthService', () {
    test('register sends credentials and returns tokens', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/register');
        final body = jsonDecode(request.body);
        expect(body['email'], 'test@example.com');
        return http.Response(
          jsonEncode({
            'accessToken': 'access-123',
            'refreshToken': 'refresh-123',
          }),
          200,
        );
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => null,
      );
      final authService = AuthService(apiClient: apiClient);

      final response = await authService.register(
        RegisterRequest(
          email: 'test@example.com',
          password: 'password123',
          name: 'Test',
        ),
      );

      expect(response.accessToken, 'access-123');
      expect(response.refreshToken, 'refresh-123');
    });

    test('login sends credentials and returns tokens', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/login');
        return http.Response(
          jsonEncode({
            'accessToken': 'access-456',
            'refreshToken': 'refresh-456',
          }),
          200,
        );
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => null,
      );
      final authService = AuthService(apiClient: apiClient);

      final response = await authService.login(
        LoginRequest(email: 'test@example.com', password: 'pass'),
      );

      expect(response.accessToken, 'access-456');
    });

    test('refresh exchanges refresh token for new pair', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/refresh');
        return http.Response(
          jsonEncode({
            'accessToken': 'new-access',
            'refreshToken': 'new-refresh',
          }),
          200,
        );
      });

      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => null,
      );
      final authService = AuthService(apiClient: apiClient);

      final response = await authService.refresh(
        RefreshRequest(refreshToken: 'old-refresh'),
      );

      expect(response.accessToken, 'new-access');
    });
  });
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd run_planner_app && flutter test test/services/auth_service_test.dart`

Expected: Compilation error — `services/auth_service.dart` does not exist.

- [ ] **Step 3: Implement the auth service**

Create `run_planner_app/lib/services/auth_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/auth.dart';
import 'api_client.dart';

class AuthService {
  final ApiClient _apiClient;

  AuthService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<AuthResponse> register(RegisterRequest request) async {
    final json = await _apiClient.post(
      ApiConfig.register,
      body: request.toJson(),
    );
    return AuthResponse.fromJson(json);
  }

  Future<AuthResponse> login(LoginRequest request) async {
    final json = await _apiClient.post(
      ApiConfig.login,
      body: request.toJson(),
    );
    return AuthResponse.fromJson(json);
  }

  Future<AuthResponse> refresh(RefreshRequest request) async {
    final json = await _apiClient.post(
      ApiConfig.refresh,
      body: request.toJson(),
    );
    return AuthResponse.fromJson(json);
  }

  Future<void> logout() async {
    await _apiClient.post(ApiConfig.logout);
  }
}
```

- [ ] **Step 4: Run auth service tests**

Run: `cd run_planner_app && flutter test test/services/auth_service_test.dart`

Expected: All 3 tests pass.

- [ ] **Step 5: Write the auth provider tests**

Create `run_planner_app/test/providers/auth_provider_test.dart`:

```dart
import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/providers/auth_provider.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/auth_service.dart';

/// A fake secure storage for testing (in-memory map).
class FakeTokenStorage implements TokenStorage {
  final Map<String, String> _store = {};

  @override
  Future<String?> read(String key) async => _store[key];

  @override
  Future<void> write(String key, String value) async => _store[key] = value;

  @override
  Future<void> delete(String key) async => _store.remove(key);
}

void main() {
  group('AuthProvider', () {
    late AuthProvider provider;
    late FakeTokenStorage storage;

    setUp(() {
      final mockClient = MockClient((request) async {
        if (request.url.path.endsWith('/login')) {
          return http.Response(
            jsonEncode({
              'accessToken': 'access-token',
              'refreshToken': 'refresh-token',
            }),
            200,
          );
        }
        if (request.url.path.endsWith('/register')) {
          return http.Response(
            jsonEncode({
              'accessToken': 'new-access',
              'refreshToken': 'new-refresh',
            }),
            200,
          );
        }
        if (request.url.path.endsWith('/logout')) {
          return http.Response('', 200);
        }
        return http.Response('Not found', 404);
      });

      storage = FakeTokenStorage();
      final apiClient = ApiClient(
        httpClient: mockClient,
        getToken: () => null,
      );
      final authService = AuthService(apiClient: apiClient);
      provider = AuthProvider(
        authService: authService,
        tokenStorage: storage,
      );
    });

    test('login stores tokens and sets authenticated', () async {
      await provider.login('test@example.com', 'password');

      expect(provider.isAuthenticated, true);
      expect(provider.accessToken, 'access-token');
      expect(await storage.read('access_token'), 'access-token');
      expect(await storage.read('refresh_token'), 'refresh-token');
    });

    test('register stores tokens and sets authenticated', () async {
      await provider.register('test@example.com', 'password', 'Test');

      expect(provider.isAuthenticated, true);
      expect(provider.accessToken, 'new-access');
    });

    test('logout clears tokens', () async {
      await provider.login('test@example.com', 'password');
      await provider.logout();

      expect(provider.isAuthenticated, false);
      expect(provider.accessToken, isNull);
      expect(await storage.read('access_token'), isNull);
    });

    test('tryRestoreSession loads tokens from storage', () async {
      await storage.write('access_token', 'stored-access');
      await storage.write('refresh_token', 'stored-refresh');

      await provider.tryRestoreSession();

      expect(provider.isAuthenticated, true);
      expect(provider.accessToken, 'stored-access');
    });

    test('tryRestoreSession with no stored tokens stays unauthenticated',
        () async {
      await provider.tryRestoreSession();

      expect(provider.isAuthenticated, false);
    });
  });
}
```

- [ ] **Step 6: Implement the auth provider**

Create `run_planner_app/lib/providers/auth_provider.dart`:

```dart
import 'package:flutter/foundation.dart';
import '../models/auth.dart';
import '../services/auth_service.dart';

/// Abstraction over secure storage so we can test without the real plugin.
abstract class TokenStorage {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
}

class AuthProvider extends ChangeNotifier {
  final AuthService _authService;
  final TokenStorage _tokenStorage;

  String? _accessToken;
  String? _refreshToken;

  AuthProvider({
    required AuthService authService,
    required TokenStorage tokenStorage,
  })  : _authService = authService,
        _tokenStorage = tokenStorage;

  bool get isAuthenticated => _accessToken != null;
  String? get accessToken => _accessToken;

  Future<void> login(String email, String password) async {
    final response = await _authService.login(
      LoginRequest(email: email, password: password),
    );
    await _storeTokens(response);
  }

  Future<void> register(String email, String password, String? name) async {
    final response = await _authService.register(
      RegisterRequest(email: email, password: password, name: name),
    );
    await _storeTokens(response);
  }

  Future<void> logout() async {
    try {
      await _authService.logout();
    } catch (_) {
      // Logout is best-effort — clear local tokens regardless
    }
    await _clearTokens();
  }

  Future<void> tryRestoreSession() async {
    final accessToken = await _tokenStorage.read('access_token');
    final refreshToken = await _tokenStorage.read('refresh_token');
    if (accessToken != null && refreshToken != null) {
      _accessToken = accessToken;
      _refreshToken = refreshToken;
      notifyListeners();
    }
  }

  Future<bool> tryRefreshToken() async {
    if (_refreshToken == null) return false;
    try {
      final response = await _authService.refresh(
        RefreshRequest(refreshToken: _refreshToken!),
      );
      await _storeTokens(response);
      return true;
    } catch (_) {
      await _clearTokens();
      return false;
    }
  }

  Future<void> _storeTokens(AuthResponse response) async {
    _accessToken = response.accessToken;
    _refreshToken = response.refreshToken;
    await _tokenStorage.write('access_token', response.accessToken);
    await _tokenStorage.write('refresh_token', response.refreshToken);
    notifyListeners();
  }

  Future<void> _clearTokens() async {
    _accessToken = null;
    _refreshToken = null;
    await _tokenStorage.delete('access_token');
    await _tokenStorage.delete('refresh_token');
    notifyListeners();
  }
}
```

- [ ] **Step 7: Run auth provider tests**

Run: `cd run_planner_app && flutter test test/providers/auth_provider_test.dart`

Expected: All 5 tests pass.

- [ ] **Step 8: Commit**

```bash
git add run_planner_app/lib/services/auth_service.dart run_planner_app/lib/providers/auth_provider.dart run_planner_app/test/services/auth_service_test.dart run_planner_app/test/providers/auth_provider_test.dart
git commit -m "feat(flutter): add auth service and provider with token persistence"
```

---

## Task 6: Remaining Services

All the API service classes that use the ApiClient to call backend endpoints. Each is a thin wrapper — call endpoint, parse response.

**Files:**
- Create: `run_planner_app/lib/services/user_service.dart`
- Create: `run_planner_app/lib/services/goal_race_service.dart`
- Create: `run_planner_app/lib/services/plan_service.dart`
- Create: `run_planner_app/lib/services/workout_service.dart`
- Create: `run_planner_app/lib/services/vdot_service.dart`
- Create: `run_planner_app/lib/services/health_sync_service.dart`
- Create: `run_planner_app/lib/services/health_kit_service.dart`
- Test: `run_planner_app/test/services/user_service_test.dart` (and others)

- [ ] **Step 1: Write user service test**

Create `run_planner_app/test/services/user_service_test.dart`:

```dart
import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/models/user.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/user_service.dart';

void main() {
  group('UserService', () {
    test('getProfile returns user', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/users/me');
        return http.Response(
          jsonEncode({
            'id': 'user-uuid',
            'email': 'test@example.com',
            'name': 'Aaron',
            'dateOfBirth': '1990-05-15',
            'maxHr': 185,
            'preferredUnits': 'METRIC',
          }),
          200,
        );
      });

      final apiClient =
          ApiClient(httpClient: mockClient, getToken: () => 'token');
      final service = UserService(apiClient: apiClient);

      final user = await service.getProfile();

      expect(user.name, 'Aaron');
      expect(user.maxHr, 185);
    });

    test('updateProfile sends patch and returns updated user', () async {
      final mockClient = MockClient((request) async {
        expect(request.method, 'PATCH');
        return http.Response(
          jsonEncode({
            'id': 'user-uuid',
            'email': 'test@example.com',
            'name': 'Updated',
            'dateOfBirth': '1990-05-15',
            'maxHr': 190,
            'preferredUnits': 'METRIC',
          }),
          200,
        );
      });

      final apiClient =
          ApiClient(httpClient: mockClient, getToken: () => 'token');
      final service = UserService(apiClient: apiClient);

      final user = await service.updateProfile(
        UpdateProfileRequest(name: 'Updated', maxHr: 190),
      );

      expect(user.name, 'Updated');
      expect(user.maxHr, 190);
    });
  });
}
```

- [ ] **Step 2: Implement all remaining services**

Create `run_planner_app/lib/services/user_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/user.dart';
import 'api_client.dart';

class UserService {
  final ApiClient _apiClient;

  UserService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<UserResponse> getProfile() async {
    final json = await _apiClient.get(ApiConfig.userProfile);
    return UserResponse.fromJson(json);
  }

  Future<UserResponse> updateProfile(UpdateProfileRequest request) async {
    final json = await _apiClient.patch(
      ApiConfig.userProfile,
      body: request.toJson(),
    );
    return UserResponse.fromJson(json);
  }
}
```

Create `run_planner_app/lib/services/goal_race_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/goal_race.dart';
import 'api_client.dart';

class GoalRaceService {
  final ApiClient _apiClient;

  GoalRaceService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<GoalRaceResponse> create(CreateGoalRaceRequest request) async {
    final json = await _apiClient.post(
      ApiConfig.goalRaces,
      body: request.toJson(),
    );
    return GoalRaceResponse.fromJson(json);
  }

  Future<List<GoalRaceResponse>> list() async {
    final jsonList = await _apiClient.getList(ApiConfig.goalRaces);
    return jsonList
        .map((j) => GoalRaceResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<GoalRaceResponse> update(
      String id, UpdateGoalRaceRequest request) async {
    final json = await _apiClient.patch(
      ApiConfig.goalRace(id),
      body: request.toJson(),
    );
    return GoalRaceResponse.fromJson(json);
  }
}
```

Create `run_planner_app/lib/services/plan_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/training_plan.dart';
import '../models/planned_workout.dart';
import 'api_client.dart';

class PlanService {
  final ApiClient _apiClient;

  PlanService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<TrainingPlanResponse> create(CreatePlanRequest request) async {
    final json = await _apiClient.post(
      ApiConfig.plans,
      body: request.toJson(),
    );
    return TrainingPlanResponse.fromJson(json);
  }

  Future<TrainingPlanResponse> getActive() async {
    final json = await _apiClient.get(ApiConfig.activePlan);
    return TrainingPlanResponse.fromJson(json);
  }

  Future<List<PlannedWorkoutResponse>> getWorkouts(
    String planId, {
    DateTime? from,
    DateTime? to,
  }) async {
    final params = <String, String>{};
    if (from != null) {
      params['from'] =
          '${from.year}-${from.month.toString().padLeft(2, '0')}-${from.day.toString().padLeft(2, '0')}';
    }
    if (to != null) {
      params['to'] =
          '${to.year}-${to.month.toString().padLeft(2, '0')}-${to.day.toString().padLeft(2, '0')}';
    }
    final jsonList = await _apiClient.getList(
      ApiConfig.planWorkouts(planId),
      queryParams: params.isNotEmpty ? params : null,
    );
    return jsonList
        .map((j) =>
            PlannedWorkoutResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<void> archive(String planId) async {
    await _apiClient.delete(ApiConfig.plan(planId));
  }
}
```

Create `run_planner_app/lib/services/workout_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/workout.dart';
import 'api_client.dart';

class WorkoutService {
  final ApiClient _apiClient;

  WorkoutService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<List<WorkoutResponse>> list({DateTime? since}) async {
    final params = <String, String>{};
    if (since != null) {
      params['since'] = since.toUtc().toIso8601String();
    }
    final jsonList = await _apiClient.getList(
      ApiConfig.workouts,
      queryParams: params.isNotEmpty ? params : null,
    );
    return jsonList
        .map((j) => WorkoutResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<WorkoutResponse> get(String id) async {
    final json = await _apiClient.get(ApiConfig.workout(id));
    return WorkoutResponse.fromJson(json);
  }
}
```

Create `run_planner_app/lib/services/vdot_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/vdot.dart';
import 'api_client.dart';

class VdotService {
  final ApiClient _apiClient;

  VdotService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<List<VdotHistoryResponse>> getHistory() async {
    final jsonList = await _apiClient.getList(ApiConfig.vdotHistory);
    return jsonList
        .map((j) => VdotHistoryResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<void> accept(String id) async {
    await _apiClient.post(ApiConfig.vdotAccept(id));
  }

  Future<void> dismiss(String id) async {
    await _apiClient.post(ApiConfig.vdotDismiss(id));
  }
}
```

Create `run_planner_app/lib/services/health_sync_service.dart`:

```dart
import '../config/api_config.dart';
import '../models/health_sync.dart';
import 'api_client.dart';

class HealthSyncService {
  final ApiClient _apiClient;

  HealthSyncService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<HealthSyncResponse> sync(HealthSyncRequest request) async {
    final json = await _apiClient.post(
      ApiConfig.healthSync,
      body: request.toJson(),
    );
    return HealthSyncResponse.fromJson(json);
  }
}
```

Create `run_planner_app/lib/services/health_kit_service.dart`:

```dart
import 'package:health/health.dart';
import '../models/health_sync.dart';

/// Reads workout and health data from Apple HealthKit.
/// This service wraps the `health` package and converts
/// HealthKit data into our sync request format.
class HealthKitService {
  final Health _health = Health();

  /// Requests read permissions for the health data types we need.
  /// Returns true if permissions were granted.
  Future<bool> requestPermissions() async {
    final types = [
      HealthDataType.WORKOUT,
      HealthDataType.HEART_RATE,
      HealthDataType.VO2MAX,
    ];
    final permissions = types.map((_) => HealthDataAccess.READ).toList();
    return await _health.requestAuthorization(types, permissions: permissions);
  }

  /// Reads workouts from HealthKit since the given timestamp.
  Future<List<WorkoutSyncItem>> getWorkouts(DateTime since) async {
    final healthData = await _health.getHealthDataFromTypes(
      types: [HealthDataType.WORKOUT],
      startTime: since,
      endTime: DateTime.now(),
    );

    return healthData
        .where((d) => d.type == HealthDataType.WORKOUT)
        .map((d) {
      final workout = d.value;
      if (workout is WorkoutHealthValue) {
        return WorkoutSyncItem(
          source: 'APPLE_HEALTH',
          sourceId: d.uuid,
          startedAt: d.dateFrom,
          distanceMeters: workout.totalDistance?.toDouble() ?? 0.0,
          durationSeconds: d.dateTo.difference(d.dateFrom).inSeconds,
          avgHr: null, // HR is read separately if needed
          maxHr: null,
          elevationGain: workout.totalEnergyBurned?.toDouble(),
        );
      }
      return null;
    })
        .whereType<WorkoutSyncItem>()
        .toList();
  }

  /// Reads the most recent VO2 max estimate from HealthKit.
  Future<HealthSnapshotSyncItem?> getLatestVo2Max(DateTime since) async {
    final data = await _health.getHealthDataFromTypes(
      types: [HealthDataType.VO2MAX],
      startTime: since,
      endTime: DateTime.now(),
    );

    if (data.isEmpty) return null;

    // Take the most recent entry
    final latest = data.last;
    final numericValue = latest.value;
    if (numericValue is NumericHealthValue) {
      return HealthSnapshotSyncItem(
        vo2maxEstimate: numericValue.numericValue.toDouble(),
        recordedAt: latest.dateFrom,
      );
    }
    return null;
  }
}
```

- [ ] **Step 3: Run user service test**

Run: `cd run_planner_app && flutter test test/services/user_service_test.dart`

Expected: All 2 tests pass.

- [ ] **Step 4: Run all tests**

Run: `cd run_planner_app && flutter test`

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add run_planner_app/lib/services/ run_planner_app/test/services/
git commit -m "feat(flutter): add all API services and HealthKit service"
```

---

## Task 7: Remaining Providers

State management providers for user, plan, workout, and sync.

**Files:**
- Create: `run_planner_app/lib/providers/user_provider.dart`
- Create: `run_planner_app/lib/providers/plan_provider.dart`
- Create: `run_planner_app/lib/providers/workout_provider.dart`
- Create: `run_planner_app/lib/providers/sync_provider.dart`
- Test: `run_planner_app/test/providers/user_provider_test.dart` (and others)

- [ ] **Step 1: Write user provider test**

Create `run_planner_app/test/providers/user_provider_test.dart`:

```dart
import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/models/user.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/user_service.dart';
import 'package:run_planner_app/services/vdot_service.dart';
import 'package:run_planner_app/providers/user_provider.dart';

void main() {
  group('UserProvider', () {
    late UserProvider provider;

    setUp(() {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/v1/users/me') {
          return http.Response(
            jsonEncode({
              'id': 'user-uuid',
              'email': 'test@example.com',
              'name': 'Aaron',
              'dateOfBirth': '1990-05-15',
              'maxHr': 185,
              'preferredUnits': 'METRIC',
            }),
            200,
          );
        }
        if (request.url.path == '/api/v1/vdot/history') {
          return http.Response(
            jsonEncode([
              {
                'id': 'v1',
                'triggeringWorkoutId': null,
                'triggeringSnapshotId': 's1',
                'previousVdot': 45.0,
                'newVdot': 48.2,
                'calculatedAt': '2026-03-25T08:00:00Z',
                'flagged': false,
                'accepted': true,
              },
            ]),
            200,
          );
        }
        return http.Response('Not found', 404);
      });

      final apiClient =
          ApiClient(httpClient: mockClient, getToken: () => 'token');
      provider = UserProvider(
        userService: UserService(apiClient: apiClient),
        vdotService: VdotService(apiClient: apiClient),
      );
    });

    test('loadProfile fetches user data', () async {
      await provider.loadProfile();

      expect(provider.user, isNotNull);
      expect(provider.user!.name, 'Aaron');
      expect(provider.user!.maxHr, 185);
    });

    test('loadVdotHistory fetches entries', () async {
      await provider.loadVdotHistory();

      expect(provider.vdotHistory, hasLength(1));
      expect(provider.vdotHistory.first.newVdot, 48.2);
    });

    test('currentVdot returns latest accepted entry', () async {
      await provider.loadVdotHistory();

      expect(provider.currentVdot, 48.2);
    });

    test('flaggedEntries returns entries needing review', () async {
      await provider.loadVdotHistory();

      expect(provider.flaggedEntries, isEmpty);
    });
  });
}
```

- [ ] **Step 2: Implement user provider**

Create `run_planner_app/lib/providers/user_provider.dart`:

```dart
import 'package:flutter/foundation.dart';
import '../models/user.dart';
import '../models/vdot.dart';
import '../services/user_service.dart';
import '../services/vdot_service.dart';

class UserProvider extends ChangeNotifier {
  final UserService _userService;
  final VdotService _vdotService;

  UserResponse? _user;
  List<VdotHistoryResponse> _vdotHistory = [];
  bool _loading = false;
  String? _error;

  UserProvider({
    required UserService userService,
    required VdotService vdotService,
  })  : _userService = userService,
        _vdotService = vdotService;

  UserResponse? get user => _user;
  List<VdotHistoryResponse> get vdotHistory => _vdotHistory;
  bool get loading => _loading;
  String? get error => _error;

  double? get currentVdot {
    final accepted = _vdotHistory.where((e) => e.accepted).toList();
    if (accepted.isEmpty) return null;
    accepted.sort((a, b) => a.calculatedAt.compareTo(b.calculatedAt));
    return accepted.last.newVdot;
  }

  List<VdotHistoryResponse> get flaggedEntries =>
      _vdotHistory.where((e) => e.flagged && !e.accepted).toList();

  Future<void> loadProfile() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _user = await _userService.getProfile();
    } catch (e) {
      _error = 'Failed to load profile';
    }
    _loading = false;
    notifyListeners();
  }

  Future<void> updateProfile(UpdateProfileRequest request) async {
    try {
      _user = await _userService.updateProfile(request);
      notifyListeners();
    } catch (e) {
      _error = 'Failed to update profile';
      notifyListeners();
    }
  }

  Future<void> loadVdotHistory() async {
    try {
      _vdotHistory = await _vdotService.getHistory();
      notifyListeners();
    } catch (e) {
      _error = 'Failed to load VDOT history';
      notifyListeners();
    }
  }

  Future<void> acceptVdot(String id) async {
    await _vdotService.accept(id);
    await loadVdotHistory();
  }

  Future<void> dismissVdot(String id) async {
    await _vdotService.dismiss(id);
    await loadVdotHistory();
  }
}
```

- [ ] **Step 3: Run user provider tests**

Run: `cd run_planner_app && flutter test test/providers/user_provider_test.dart`

Expected: All 4 tests pass.

- [ ] **Step 4: Implement plan provider**

Create `run_planner_app/lib/providers/plan_provider.dart`:

```dart
import 'package:flutter/foundation.dart';
import '../models/training_plan.dart';
import '../models/planned_workout.dart';
import '../models/goal_race.dart';
import '../services/plan_service.dart';
import '../services/goal_race_service.dart';

class PlanProvider extends ChangeNotifier {
  final PlanService _planService;
  final GoalRaceService _goalRaceService;

  TrainingPlanResponse? _activePlan;
  GoalRaceResponse? _activeGoalRace;
  List<PlannedWorkoutResponse> _weekWorkouts = [];
  List<GoalRaceResponse> _goalRaces = [];
  bool _loading = false;
  String? _error;

  PlanProvider({
    required PlanService planService,
    required GoalRaceService goalRaceService,
  })  : _planService = planService,
        _goalRaceService = goalRaceService;

  TrainingPlanResponse? get activePlan => _activePlan;
  GoalRaceResponse? get activeGoalRace => _activeGoalRace;
  List<PlannedWorkoutResponse> get weekWorkouts => _weekWorkouts;
  List<GoalRaceResponse> get goalRaces => _goalRaces;
  bool get loading => _loading;
  String? get error => _error;
  bool get hasActivePlan => _activePlan != null;

  int get totalWeeks {
    if (_activePlan == null) return 0;
    return _activePlan!.endDate
            .difference(_activePlan!.startDate)
            .inDays ~/
        7;
  }

  int get currentWeek {
    if (_activePlan == null) return 0;
    final daysSinceStart =
        DateTime.now().difference(_activePlan!.startDate).inDays;
    return (daysSinceStart ~/ 7) + 1;
  }

  double get progressPercent {
    if (totalWeeks == 0) return 0;
    return (currentWeek / totalWeeks).clamp(0.0, 1.0);
  }

  PlannedWorkoutResponse? get todayWorkout {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    try {
      return _activePlan?.workouts.firstWhere(
        (w) => w.scheduledDate.isAtSameMomentAs(today),
      );
    } catch (_) {
      return null;
    }
  }

  Future<void> loadActivePlan() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _activePlan = await _planService.getActive();
      await _loadActiveGoalRace();
    } catch (e) {
      _activePlan = null;
      _error = null; // No active plan is not an error
    }
    _loading = false;
    notifyListeners();
  }

  Future<void> loadWeekWorkouts(String planId, DateTime weekStart) async {
    try {
      final weekEnd = weekStart.add(const Duration(days: 6));
      _weekWorkouts = await _planService.getWorkouts(
        planId,
        from: weekStart,
        to: weekEnd,
      );
      notifyListeners();
    } catch (e) {
      _error = 'Failed to load workouts';
      notifyListeners();
    }
  }

  Future<void> loadGoalRaces() async {
    try {
      _goalRaces = await _goalRaceService.list();
      notifyListeners();
    } catch (e) {
      _error = 'Failed to load goal races';
      notifyListeners();
    }
  }

  Future<GoalRaceResponse> createGoalRace(
      CreateGoalRaceRequest request) async {
    final race = await _goalRaceService.create(request);
    await loadGoalRaces();
    return race;
  }

  Future<TrainingPlanResponse> createPlan(String goalRaceId) async {
    final plan =
        await _planService.create(CreatePlanRequest(goalRaceId: goalRaceId));
    _activePlan = plan;
    notifyListeners();
    return plan;
  }

  Future<void> _loadActiveGoalRace() async {
    if (_activePlan == null) return;
    await loadGoalRaces();
    try {
      _activeGoalRace = _goalRaces.firstWhere(
        (r) => r.id == _activePlan!.goalRaceId,
      );
    } catch (_) {
      _activeGoalRace = null;
    }
  }
}
```

- [ ] **Step 5: Implement workout provider**

Create `run_planner_app/lib/providers/workout_provider.dart`:

```dart
import 'package:flutter/foundation.dart';
import '../models/workout.dart';
import '../services/workout_service.dart';

enum WorkoutFilter { all, matched, flagged }

class WorkoutProvider extends ChangeNotifier {
  final WorkoutService _workoutService;

  List<WorkoutResponse> _workouts = [];
  WorkoutFilter _filter = WorkoutFilter.all;
  bool _loading = false;
  String? _error;

  WorkoutProvider({required WorkoutService workoutService})
      : _workoutService = workoutService;

  List<WorkoutResponse> get workouts => _workouts;
  WorkoutFilter get filter => _filter;
  bool get loading => _loading;
  String? get error => _error;

  void setFilter(WorkoutFilter filter) {
    _filter = filter;
    notifyListeners();
  }

  Future<void> loadWorkouts({DateTime? since}) async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _workouts = await _workoutService.list(since: since);
    } catch (e) {
      _error = 'Failed to load workouts';
    }
    _loading = false;
    notifyListeners();
  }
}
```

- [ ] **Step 6: Implement sync provider**

Create `run_planner_app/lib/providers/sync_provider.dart`:

```dart
import 'package:flutter/foundation.dart';
import '../models/health_sync.dart';
import '../services/health_kit_service.dart';
import '../services/health_sync_service.dart';

class SyncProvider extends ChangeNotifier {
  final HealthKitService _healthKitService;
  final HealthSyncService _healthSyncService;

  DateTime? _lastSyncedAt;
  bool _syncing = false;
  String? _error;
  HealthSyncResponse? _lastSyncResult;

  SyncProvider({
    required HealthKitService healthKitService,
    required HealthSyncService healthSyncService,
  })  : _healthKitService = healthKitService,
        _healthSyncService = healthSyncService;

  DateTime? get lastSyncedAt => _lastSyncedAt;
  bool get syncing => _syncing;
  String? get error => _error;
  HealthSyncResponse? get lastSyncResult => _lastSyncResult;

  Future<bool> requestPermissions() async {
    return await _healthKitService.requestPermissions();
  }

  Future<void> sync({DateTime? since}) async {
    if (_syncing) return;

    _syncing = true;
    _error = null;
    notifyListeners();

    try {
      final syncSince =
          since ?? _lastSyncedAt ?? DateTime.now().subtract(const Duration(days: 30));

      final workouts = await _healthKitService.getWorkouts(syncSince);
      final vo2max = await _healthKitService.getLatestVo2Max(syncSince);

      final request = HealthSyncRequest(
        workouts: workouts,
        healthSnapshots: vo2max != null ? [vo2max] : [],
      );

      _lastSyncResult = await _healthSyncService.sync(request);
      _lastSyncedAt = DateTime.now();
    } catch (e) {
      _error = 'Sync failed';
    }

    _syncing = false;
    notifyListeners();
  }
}
```

- [ ] **Step 7: Run all tests**

Run: `cd run_planner_app && flutter test`

Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
git add run_planner_app/lib/providers/ run_planner_app/test/providers/
git commit -m "feat(flutter): add user, plan, workout, and sync providers"
```

---

## Task 8: Reusable Widgets

The shared UI components used across multiple screens.

**Files:**
- Create: `run_planner_app/lib/widgets/race_countdown_card.dart`
- Create: `run_planner_app/lib/widgets/workout_card.dart`
- Create: `run_planner_app/lib/widgets/compliance_bars.dart`
- Create: `run_planner_app/lib/widgets/week_day_indicators.dart`
- Create: `run_planner_app/lib/widgets/sync_indicator.dart`
- Create: `run_planner_app/lib/widgets/notification_banner.dart`
- Create: `run_planner_app/lib/widgets/vdot_chart.dart`
- Test: `run_planner_app/test/widgets/race_countdown_card_test.dart` (and others)

- [ ] **Step 1: Write race countdown card test**

Create `run_planner_app/test/widgets/race_countdown_card_test.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/widgets/race_countdown_card.dart';

void main() {
  group('RaceCountdownCard', () {
    testWidgets('displays race name and days remaining', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: RaceCountdownCard(
              raceName: 'Chicago Marathon',
              raceDate: '2026-10-12',
              progressPercent: 0.71,
            ),
          ),
        ),
      );

      expect(find.text('Chicago Marathon'), findsOneWidget);
      expect(find.textContaining('Oct 12'), findsOneWidget);
    });

    testWidgets('shows progress percentage', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: RaceCountdownCard(
              raceName: 'Test Race',
              raceDate: '2026-12-01',
              progressPercent: 0.50,
            ),
          ),
        ),
      );

      expect(find.textContaining('50%'), findsOneWidget);
    });
  });
}
```

- [ ] **Step 2: Implement race countdown card**

Create `run_planner_app/lib/widgets/race_countdown_card.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../theme/app_theme.dart';

class RaceCountdownCard extends StatelessWidget {
  final String raceName;
  final String raceDate;
  final double progressPercent;

  const RaceCountdownCard({
    super.key,
    required this.raceName,
    required this.raceDate,
    required this.progressPercent,
  });

  @override
  Widget build(BuildContext context) {
    final date = DateTime.parse(raceDate);
    final daysAway = date.difference(DateTime.now()).inDays;
    final formattedDate = DateFormat('MMM d').format(date);
    final percentText = '${(progressPercent * 100).round()}%';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              raceName,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: AppTheme.textPrimary,
              ),
            ),
            const SizedBox(height: AppTheme.spacingXs),
            Text(
              '$daysAway days away \u00b7 $formattedDate',
              style: const TextStyle(
                fontSize: 14,
                color: AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: AppTheme.spacingSm),
            Row(
              children: [
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: progressPercent,
                      minHeight: 8,
                      backgroundColor: AppTheme.divider,
                      valueColor: const AlwaysStoppedAnimation<Color>(
                          AppTheme.primary),
                    ),
                  ),
                ),
                const SizedBox(width: AppTheme.spacingSm),
                Text(
                  '$percentText through',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
```

- [ ] **Step 3: Run race countdown card test**

Run: `cd run_planner_app && flutter test test/widgets/race_countdown_card_test.dart`

Expected: Both tests pass.

- [ ] **Step 4: Implement remaining widgets**

Create `run_planner_app/lib/widgets/workout_card.dart`:

```dart
import 'package:flutter/material.dart';
import '../models/planned_workout.dart';
import '../theme/app_theme.dart';

class WorkoutCard extends StatelessWidget {
  final PlannedWorkoutResponse workout;
  final double? complianceScore;
  final bool isToday;
  final bool isMissed;
  final VoidCallback? onTap;

  const WorkoutCard({
    super.key,
    required this.workout,
    this.complianceScore,
    this.isToday = false,
    this.isMissed = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spacingMd,
          vertical: AppTheme.spacingSm,
        ),
        decoration: BoxDecoration(
          color: isToday ? AppTheme.primaryLight : null,
          border: Border(
            bottom: BorderSide(color: AppTheme.divider, width: 0.5),
          ),
        ),
        child: Row(
          children: [
            _buildStatusIcon(),
            const SizedBox(width: AppTheme.spacingSm),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    workout.workoutType.displayName,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight:
                          isToday ? FontWeight.w600 : FontWeight.normal,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  if (workout.workoutType != WorkoutType.rest)
                    Text(
                      _subtitle(),
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                ],
              ),
            ),
            if (complianceScore != null) _buildComplianceBadge(),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusIcon() {
    if (complianceScore != null) {
      return Icon(Icons.check_circle, color: _complianceColor(), size: 20);
    }
    if (isMissed) {
      return const Icon(Icons.cancel, color: AppTheme.error, size: 20);
    }
    if (isToday) {
      return const Icon(Icons.circle, color: AppTheme.primary, size: 20);
    }
    return const Icon(Icons.circle_outlined,
        color: AppTheme.textSecondary, size: 20);
  }

  Widget _buildComplianceBadge() {
    final pct = '${(complianceScore! * 100).round()}%';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: _complianceColor().withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        pct,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: _complianceColor(),
        ),
      ),
    );
  }

  Color _complianceColor() {
    if (complianceScore == null) return AppTheme.textSecondary;
    if (complianceScore! >= 0.8) return AppTheme.success;
    if (complianceScore! >= 0.6) return AppTheme.warning;
    return AppTheme.error;
  }

  String _subtitle() {
    final km = (workout.targetDistanceMeters / 1000).toStringAsFixed(1);
    if (workout.targetPaceMinPerKm != null &&
        workout.targetPaceMaxPerKm != null) {
      return '$km km \u00b7 ${_formatPace(workout.targetPaceMinPerKm!)}-${_formatPace(workout.targetPaceMaxPerKm!)}/km';
    }
    return '$km km';
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
```

Create `run_planner_app/lib/widgets/compliance_bars.dart`:

```dart
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class ComplianceBars extends StatelessWidget {
  final double distanceScore;
  final double paceScore;
  final double hrScore;
  final double overallScore;

  const ComplianceBars({
    super.key,
    required this.distanceScore,
    required this.paceScore,
    required this.hrScore,
    required this.overallScore,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildOverall(),
        const SizedBox(height: AppTheme.spacingMd),
        _buildBar('Distance', distanceScore, '40%'),
        const SizedBox(height: AppTheme.spacingSm),
        _buildBar('Pace', paceScore, '40%'),
        const SizedBox(height: AppTheme.spacingSm),
        _buildBar('HR Zone', hrScore, '20%'),
      ],
    );
  }

  Widget _buildOverall() {
    final pct = '${(overallScore * 100).round()}%';
    return Row(
      children: [
        const Text(
          'COMPLIANCE',
          style: TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            letterSpacing: 1,
            color: AppTheme.textSecondary,
          ),
        ),
        const SizedBox(width: AppTheme.spacingSm),
        Text(
          pct,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: _scoreColor(overallScore),
          ),
        ),
      ],
    );
  }

  Widget _buildBar(String label, double score, String weight) {
    final pct = '${(score * 100).round()}%';
    return Row(
      children: [
        SizedBox(
          width: 70,
          child: Text(
            label,
            style: const TextStyle(
              fontSize: 13,
              color: AppTheme.textSecondary,
            ),
          ),
        ),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: score,
              minHeight: 8,
              backgroundColor: AppTheme.divider,
              valueColor: AlwaysStoppedAnimation<Color>(_scoreColor(score)),
            ),
          ),
        ),
        const SizedBox(width: AppTheme.spacingSm),
        SizedBox(
          width: 35,
          child: Text(
            pct,
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
        ),
      ],
    );
  }

  Color _scoreColor(double score) {
    if (score >= 0.8) return AppTheme.success;
    if (score >= 0.6) return AppTheme.warning;
    return AppTheme.error;
  }
}
```

Create `run_planner_app/lib/widgets/week_day_indicators.dart`:

```dart
import 'package:flutter/material.dart';
import '../models/planned_workout.dart';
import '../theme/app_theme.dart';

class DayStatus {
  final String dayLabel;
  final WorkoutType workoutType;
  final bool isCompleted;
  final bool isMissed;
  final bool isToday;

  const DayStatus({
    required this.dayLabel,
    required this.workoutType,
    this.isCompleted = false,
    this.isMissed = false,
    this.isToday = false,
  });
}

class WeekDayIndicators extends StatelessWidget {
  final List<DayStatus> days;

  const WeekDayIndicators({super.key, required this.days});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: days.map(_buildDay).toList(),
    );
  }

  Widget _buildDay(DayStatus day) {
    return Column(
      children: [
        Text(
          day.dayLabel,
          style: TextStyle(
            fontSize: 11,
            fontWeight: day.isToday ? FontWeight.w700 : FontWeight.normal,
            color: day.isToday ? AppTheme.primary : AppTheme.textSecondary,
          ),
        ),
        const SizedBox(height: 4),
        _buildIcon(day),
        const SizedBox(height: 2),
        Text(
          day.workoutType.abbreviation,
          style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
        ),
      ],
    );
  }

  Widget _buildIcon(DayStatus day) {
    if (day.isCompleted) {
      return const Icon(Icons.check_circle, color: AppTheme.success, size: 18);
    }
    if (day.isMissed) {
      return const Icon(Icons.cancel, color: AppTheme.error, size: 18);
    }
    if (day.isToday) {
      return const Icon(Icons.circle, color: AppTheme.primary, size: 18);
    }
    return const Icon(Icons.circle_outlined,
        color: AppTheme.textSecondary, size: 18);
  }
}
```

Create `run_planner_app/lib/widgets/sync_indicator.dart`:

```dart
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class SyncIndicator extends StatelessWidget {
  final DateTime? lastSyncedAt;
  final bool syncing;
  final String? error;

  const SyncIndicator({
    super.key,
    this.lastSyncedAt,
    this.syncing = false,
    this.error,
  });

  @override
  Widget build(BuildContext context) {
    if (syncing) {
      return const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: 12,
            height: 12,
            child: CircularProgressIndicator(strokeWidth: 1.5),
          ),
          SizedBox(width: 6),
          Text(
            'Syncing...',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
        ],
      );
    }

    if (error != null) {
      return Text(
        'Sync failed',
        style: TextStyle(fontSize: 12, color: AppTheme.error),
      );
    }

    if (lastSyncedAt == null) {
      return const Text(
        'Not synced yet',
        style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
      );
    }

    final diff = DateTime.now().difference(lastSyncedAt!);
    final text = _formatDuration(diff);

    return Text(
      'Last synced: $text ago',
      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
    );
  }

  String _formatDuration(Duration diff) {
    if (diff.inMinutes < 1) return 'just now';
    if (diff.inMinutes < 60) return '${diff.inMinutes}m';
    if (diff.inHours < 24) return '${diff.inHours}h';
    return '${diff.inDays}d';
  }
}
```

Create `run_planner_app/lib/widgets/notification_banner.dart`:

```dart
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class NotificationBanner extends StatelessWidget {
  final String message;
  final String? actionLabel;
  final VoidCallback? onTap;
  final VoidCallback? onDismiss;

  const NotificationBanner({
    super.key,
    required this.message,
    this.actionLabel,
    this.onTap,
    this.onDismiss,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      color: AppTheme.warning.withValues(alpha: 0.1),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppTheme.radiusMd),
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spacingSm),
          child: Row(
            children: [
              const Icon(Icons.warning_amber_rounded,
                  color: AppTheme.warning, size: 20),
              const SizedBox(width: AppTheme.spacingSm),
              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(
                    fontSize: 14,
                    color: AppTheme.textPrimary,
                  ),
                ),
              ),
              if (actionLabel != null)
                Text(
                  actionLabel!,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: AppTheme.primary,
                  ),
                ),
              if (onDismiss != null)
                IconButton(
                  icon: const Icon(Icons.close, size: 18),
                  onPressed: onDismiss,
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
```

Create `run_planner_app/lib/widgets/vdot_chart.dart`:

```dart
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/vdot.dart';
import '../theme/app_theme.dart';

class VdotChart extends StatelessWidget {
  final List<VdotHistoryResponse> history;
  final void Function(VdotHistoryResponse entry)? onPointTap;

  const VdotChart({super.key, required this.history, this.onPointTap});

  @override
  Widget build(BuildContext context) {
    if (history.isEmpty) {
      return const SizedBox(
        height: 200,
        child: Center(
          child: Text('No VDOT history yet',
              style: TextStyle(color: AppTheme.textSecondary)),
        ),
      );
    }

    final accepted = history.where((e) => e.accepted).toList()
      ..sort((a, b) => a.calculatedAt.compareTo(b.calculatedAt));

    if (accepted.isEmpty) {
      return const SizedBox(
        height: 200,
        child: Center(
          child: Text('No accepted VDOT data',
              style: TextStyle(color: AppTheme.textSecondary)),
        ),
      );
    }

    final spots = accepted.asMap().entries.map((e) {
      return FlSpot(e.key.toDouble(), e.value.newVdot);
    }).toList();

    final minY = spots.map((s) => s.y).reduce((a, b) => a < b ? a : b) - 2;
    final maxY = spots.map((s) => s.y).reduce((a, b) => a > b ? a : b) + 2;

    return SizedBox(
      height: 200,
      child: LineChart(
        LineChartData(
          minY: minY,
          maxY: maxY,
          gridData: const FlGridData(show: false),
          titlesData: FlTitlesData(
            topTitles:
                const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles:
                const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                getTitlesWidget: (value, meta) {
                  final idx = value.toInt();
                  if (idx < 0 || idx >= accepted.length) {
                    return const SizedBox.shrink();
                  }
                  return Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      DateFormat('MMM').format(accepted[idx].calculatedAt),
                      style: const TextStyle(
                        fontSize: 10,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  );
                },
              ),
            ),
            leftTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 35,
                getTitlesWidget: (value, meta) {
                  return Text(
                    value.toInt().toString(),
                    style: const TextStyle(
                      fontSize: 10,
                      color: AppTheme.textSecondary,
                    ),
                  );
                },
              ),
            ),
          ),
          borderData: FlBorderData(show: false),
          lineBarsData: [
            LineChartBarData(
              spots: spots,
              isCurved: true,
              color: AppTheme.primary,
              barWidth: 2,
              dotData: FlDotData(
                show: true,
                getDotPainter: (spot, _, __, ___) {
                  return FlDotCirclePainter(
                    radius: 4,
                    color: AppTheme.primary,
                    strokeWidth: 2,
                    strokeColor: Colors.white,
                  );
                },
              ),
              belowBarData: BarAreaData(
                show: true,
                color: AppTheme.primary.withValues(alpha: 0.1),
              ),
            ),
          ],
          lineTouchData: LineTouchData(
            touchCallback: (event, response) {
              if (event is FlTapUpEvent &&
                  response?.lineBarSpots != null &&
                  response!.lineBarSpots!.isNotEmpty &&
                  onPointTap != null) {
                final idx = response.lineBarSpots!.first.spotIndex;
                if (idx < accepted.length) {
                  onPointTap!(accepted[idx]);
                }
              }
            },
            touchTooltipData: LineTouchTooltipData(
              getTooltipItems: (spots) {
                return spots.map((spot) {
                  return LineTooltipItem(
                    'VDOT ${spot.y.toStringAsFixed(1)}',
                    const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                    ),
                  );
                }).toList();
              },
            ),
          ),
        ),
      ),
    );
  }
}
```

- [ ] **Step 5: Run all tests**

Run: `cd run_planner_app && flutter test`

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add run_planner_app/lib/widgets/ run_planner_app/test/widgets/
git commit -m "feat(flutter): add reusable widgets (cards, indicators, chart)"
```

---

## Task 9: Onboarding Screens

The linear flow users see the first time they open the app.

**Files:**
- Create: `run_planner_app/lib/screens/onboarding/welcome_screen.dart`
- Create: `run_planner_app/lib/screens/onboarding/profile_setup_screen.dart`
- Create: `run_planner_app/lib/screens/onboarding/health_permission_screen.dart`
- Create: `run_planner_app/lib/screens/onboarding/initial_sync_screen.dart`
- Create: `run_planner_app/lib/screens/onboarding/goal_race_wizard_screen.dart`

- [ ] **Step 1: Implement the welcome/login/register screen**

Create `run_planner_app/lib/screens/onboarding/welcome_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';

class WelcomeScreen extends StatefulWidget {
  final VoidCallback onAuthenticated;

  const WelcomeScreen({super.key, required this.onAuthenticated});

  @override
  State<WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  bool _isLogin = true;
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _nameController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final auth = context.read<AuthProvider>();
      if (_isLogin) {
        await auth.login(
          _emailController.text.trim(),
          _passwordController.text,
        );
      } else {
        await auth.register(
          _emailController.text.trim(),
          _passwordController.text,
          _nameController.text.trim().isNotEmpty
              ? _nameController.text.trim()
              : null,
        );
      }
      widget.onAuthenticated();
    } catch (e) {
      setState(() {
        _error = _isLogin
            ? 'Invalid email or password'
            : 'Registration failed. Try a different email.';
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 60),
                const Text(
                  'Run Planner',
                  style: TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.w700,
                    color: AppTheme.textPrimary,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                const Text(
                  'Your personal running coach',
                  style: TextStyle(
                    fontSize: 16,
                    color: AppTheme.textSecondary,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 48),
                if (!_isLogin) ...[
                  TextFormField(
                    controller: _nameController,
                    decoration: const InputDecoration(labelText: 'Name'),
                    textCapitalization: TextCapitalization.words,
                  ),
                  const SizedBox(height: AppTheme.spacingMd),
                ],
                TextFormField(
                  controller: _emailController,
                  decoration: const InputDecoration(labelText: 'Email'),
                  keyboardType: TextInputType.emailAddress,
                  validator: (v) =>
                      v != null && v.contains('@') ? null : 'Enter a valid email',
                ),
                const SizedBox(height: AppTheme.spacingMd),
                TextFormField(
                  controller: _passwordController,
                  decoration: const InputDecoration(labelText: 'Password'),
                  obscureText: true,
                  validator: (v) => v != null && v.length >= 8
                      ? null
                      : 'At least 8 characters',
                ),
                const SizedBox(height: AppTheme.spacingLg),
                if (_error != null) ...[
                  Text(
                    _error!,
                    style: const TextStyle(color: AppTheme.error),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: AppTheme.spacingSm),
                ],
                ElevatedButton(
                  onPressed: _loading ? null : _submit,
                  child: _loading
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child:
                              CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Text(_isLogin ? 'Log In' : 'Create Account'),
                ),
                const SizedBox(height: AppTheme.spacingMd),
                TextButton(
                  onPressed: () => setState(() {
                    _isLogin = !_isLogin;
                    _error = null;
                  }),
                  child: Text(_isLogin
                      ? 'Don\'t have an account? Sign up'
                      : 'Already have an account? Log in'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
```

- [ ] **Step 2: Implement the profile setup screen**

Create `run_planner_app/lib/screens/onboarding/profile_setup_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/user.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';

class ProfileSetupScreen extends StatefulWidget {
  final VoidCallback onComplete;

  const ProfileSetupScreen({super.key, required this.onComplete});

  @override
  State<ProfileSetupScreen> createState() => _ProfileSetupScreenState();
}

class _ProfileSetupScreenState extends State<ProfileSetupScreen> {
  final _nameController = TextEditingController();
  final _maxHrController = TextEditingController();
  DateTime? _dateOfBirth;
  Units _units = Units.metric;
  bool _loading = false;

  @override
  void dispose() {
    _nameController.dispose();
    _maxHrController.dispose();
    super.dispose();
  }

  Future<void> _pickDateOfBirth() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime(1990, 1, 1),
      firstDate: DateTime(1940),
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() => _dateOfBirth = picked);
    }
  }

  Future<void> _submit() async {
    setState(() => _loading = true);
    try {
      final maxHr = _maxHrController.text.isNotEmpty
          ? int.tryParse(_maxHrController.text)
          : null;
      await context.read<UserProvider>().updateProfile(
            UpdateProfileRequest(
              name: _nameController.text.trim().isNotEmpty
                  ? _nameController.text.trim()
                  : null,
              dateOfBirth: _dateOfBirth,
              maxHr: maxHr,
              preferredUnits: _units,
            ),
          );
      widget.onComplete();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Set Up Profile')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Tell us about yourself',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: AppTheme.spacingLg),
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Name'),
                textCapitalization: TextCapitalization.words,
              ),
              const SizedBox(height: AppTheme.spacingMd),
              InkWell(
                onTap: _pickDateOfBirth,
                child: InputDecorator(
                  decoration:
                      const InputDecoration(labelText: 'Date of Birth'),
                  child: Text(
                    _dateOfBirth != null
                        ? '${_dateOfBirth!.month}/${_dateOfBirth!.day}/${_dateOfBirth!.year}'
                        : 'Tap to select',
                    style: TextStyle(
                      color: _dateOfBirth != null
                          ? AppTheme.textPrimary
                          : AppTheme.textSecondary,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: AppTheme.spacingMd),
              TextFormField(
                controller: _maxHrController,
                decoration: const InputDecoration(
                  labelText: 'Max Heart Rate (optional)',
                  hintText: 'e.g. 185',
                ),
                keyboardType: TextInputType.number,
              ),
              const SizedBox(height: AppTheme.spacingMd),
              const Text('Preferred Units',
                  style: TextStyle(color: AppTheme.textSecondary)),
              const SizedBox(height: AppTheme.spacingSm),
              SegmentedButton<Units>(
                segments: const [
                  ButtonSegment(value: Units.metric, label: Text('Kilometers')),
                  ButtonSegment(
                      value: Units.imperial, label: Text('Miles')),
                ],
                selected: {_units},
                onSelectionChanged: (v) => setState(() => _units = v.first),
              ),
              const SizedBox(height: AppTheme.spacingXl),
              ElevatedButton(
                onPressed: _loading ? null : _submit,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Continue'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
```

- [ ] **Step 3: Implement the health permission screen**

Create `run_planner_app/lib/screens/onboarding/health_permission_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/sync_provider.dart';
import '../../theme/app_theme.dart';

class HealthPermissionScreen extends StatefulWidget {
  final VoidCallback onComplete;

  const HealthPermissionScreen({super.key, required this.onComplete});

  @override
  State<HealthPermissionScreen> createState() => _HealthPermissionScreenState();
}

class _HealthPermissionScreenState extends State<HealthPermissionScreen> {
  bool _requesting = false;

  Future<void> _requestPermissions() async {
    setState(() => _requesting = true);
    try {
      await context.read<SyncProvider>().requestPermissions();
      widget.onComplete();
    } finally {
      if (mounted) setState(() => _requesting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Health Data')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 40),
              const Icon(Icons.favorite, size: 64, color: AppTheme.error),
              const SizedBox(height: AppTheme.spacingLg),
              const Text(
                'Connect Apple Health',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  color: AppTheme.textPrimary,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: AppTheme.spacingMd),
              const Text(
                'Run Planner reads your workout history, heart rate, and VO2 max '
                'to calculate your fitness level and match your runs against '
                'your training plan.\n\n'
                'We only read data — we never write to Apple Health.',
                style: TextStyle(
                  fontSize: 15,
                  color: AppTheme.textSecondary,
                  height: 1.5,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(),
              ElevatedButton(
                onPressed: _requesting ? null : _requestPermissions,
                child: _requesting
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Allow Access'),
              ),
              const SizedBox(height: AppTheme.spacingSm),
              TextButton(
                onPressed: widget.onComplete,
                child: const Text('Skip for Now'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
```

- [ ] **Step 4: Implement the initial sync screen**

Create `run_planner_app/lib/screens/onboarding/initial_sync_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/sync_provider.dart';
import '../../theme/app_theme.dart';

class InitialSyncScreen extends StatefulWidget {
  final VoidCallback onComplete;

  const InitialSyncScreen({super.key, required this.onComplete});

  @override
  State<InitialSyncScreen> createState() => _InitialSyncScreenState();
}

class _InitialSyncScreenState extends State<InitialSyncScreen> {
  @override
  void initState() {
    super.initState();
    _runSync();
  }

  Future<void> _runSync() async {
    final syncProvider = context.read<SyncProvider>();
    await syncProvider.sync(
      since: DateTime.now().subtract(const Duration(days: 90)),
    );
    if (mounted) widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(AppTheme.spacingLg),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const CircularProgressIndicator(),
                const SizedBox(height: AppTheme.spacingLg),
                const Text(
                  'Pulling your recent workouts...',
                  style: TextStyle(
                    fontSize: 18,
                    color: AppTheme.textPrimary,
                  ),
                ),
                const SizedBox(height: AppTheme.spacingSm),
                const Text(
                  'This may take a moment',
                  style: TextStyle(
                    fontSize: 14,
                    color: AppTheme.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
```

- [ ] **Step 5: Implement the goal race wizard screen**

Create `run_planner_app/lib/screens/onboarding/goal_race_wizard_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/goal_race.dart';
import '../../providers/plan_provider.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';

class _RaceOption {
  final String label;
  final int distanceMeters;

  const _RaceOption(this.label, this.distanceMeters);
}

class GoalRaceWizardScreen extends StatefulWidget {
  final VoidCallback onComplete;

  const GoalRaceWizardScreen({super.key, required this.onComplete});

  @override
  State<GoalRaceWizardScreen> createState() => _GoalRaceWizardScreenState();
}

class _GoalRaceWizardScreenState extends State<GoalRaceWizardScreen> {
  int _step = 0;
  _RaceOption? _selectedDistance;
  DateTime? _raceDate;
  int? _goalFinishSeconds;
  bool _loading = false;

  final _hoursController = TextEditingController();
  final _minutesController = TextEditingController();

  static const _distances = [
    _RaceOption('5K', 5000),
    _RaceOption('10K', 10000),
    _RaceOption('Half Marathon', 21097),
    _RaceOption('Marathon', 42195),
  ];

  @override
  void dispose() {
    _hoursController.dispose();
    _minutesController.dispose();
    super.dispose();
  }

  Future<void> _generatePlan() async {
    if (_selectedDistance == null || _raceDate == null) return;

    setState(() => _loading = true);

    try {
      final planProvider = context.read<PlanProvider>();

      // Parse goal time
      final hours = int.tryParse(_hoursController.text) ?? 0;
      final minutes = int.tryParse(_minutesController.text) ?? 0;
      if (hours > 0 || minutes > 0) {
        _goalFinishSeconds = (hours * 3600) + (minutes * 60);
      }

      final race = await planProvider.createGoalRace(
        CreateGoalRaceRequest(
          distanceMeters: _selectedDistance!.distanceMeters,
          distanceLabel: _selectedDistance!.label,
          raceDate: _raceDate!,
          goalFinishSeconds: _goalFinishSeconds,
        ),
      );

      await planProvider.createPlan(race.id);
      widget.onComplete();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Step ${_step + 1} of 4'),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(AppTheme.spacingLg),
          child: switch (_step) {
            0 => _buildDistanceStep(),
            1 => _buildDateStep(),
            2 => _buildGoalTimeStep(),
            3 => _buildConfirmationStep(),
            _ => const SizedBox.shrink(),
          },
        ),
      ),
    );
  }

  Widget _buildDistanceStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'What are you training for?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        ..._distances.map((d) => Padding(
              padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
              child: OutlinedButton(
                onPressed: () {
                  setState(() {
                    _selectedDistance = d;
                    _step = 1;
                  });
                },
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.all(AppTheme.spacingMd),
                  alignment: Alignment.centerLeft,
                ),
                child: Text(
                  d.label,
                  style: const TextStyle(fontSize: 18),
                ),
              ),
            )),
      ],
    );
  }

  Widget _buildDateStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'When is your race?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        CalendarDatePicker(
          initialDate: DateTime.now().add(const Duration(days: 90)),
          firstDate: DateTime.now().add(const Duration(days: 14)),
          lastDate: DateTime.now().add(const Duration(days: 365)),
          onDateChanged: (date) {
            setState(() {
              _raceDate = date;
              _step = 2;
            });
          },
        ),
      ],
    );
  }

  Widget _buildGoalTimeStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'What\'s your goal finish time?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingSm),
        const Text(
          'Optional — leave blank if you\'re not targeting a specific time',
          style: TextStyle(color: AppTheme.textSecondary),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        Row(
          children: [
            Expanded(
              child: TextFormField(
                controller: _hoursController,
                decoration: const InputDecoration(
                  labelText: 'Hours',
                  hintText: '3',
                ),
                keyboardType: TextInputType.number,
              ),
            ),
            const SizedBox(width: AppTheme.spacingMd),
            Expanded(
              child: TextFormField(
                controller: _minutesController,
                decoration: const InputDecoration(
                  labelText: 'Minutes',
                  hintText: '15',
                ),
                keyboardType: TextInputType.number,
              ),
            ),
          ],
        ),
        const SizedBox(height: AppTheme.spacingXl),
        ElevatedButton(
          onPressed: () => setState(() => _step = 3),
          child: const Text('Continue'),
        ),
      ],
    );
  }

  Widget _buildConfirmationStep() {
    final userProvider = context.watch<UserProvider>();
    final vdot = userProvider.currentVdot;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Ready to generate your plan',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w700,
            color: AppTheme.textPrimary,
          ),
        ),
        const SizedBox(height: AppTheme.spacingLg),
        _buildSummaryRow('Race', _selectedDistance!.label),
        _buildSummaryRow(
            'Date',
            '${_raceDate!.month}/${_raceDate!.day}/${_raceDate!.year}'),
        if (_hoursController.text.isNotEmpty ||
            _minutesController.text.isNotEmpty)
          _buildSummaryRow(
              'Goal', '${_hoursController.text}h ${_minutesController.text}m'),
        if (vdot != null)
          _buildSummaryRow('Current VDOT', vdot.toStringAsFixed(1)),
        const Spacer(),
        ElevatedButton(
          onPressed: _loading ? null : _generatePlan,
          child: _loading
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Generate Plan'),
        ),
      ],
    );
  }

  Widget _buildSummaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label,
              style: const TextStyle(
                  fontSize: 16, color: AppTheme.textSecondary)),
          Text(value,
              style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary)),
        ],
      ),
    );
  }
}
```

- [ ] **Step 6: Verify compilation**

Run: `cd run_planner_app && flutter analyze`

Expected: "No issues found!"

- [ ] **Step 7: Commit**

```bash
git add run_planner_app/lib/screens/onboarding/
git commit -m "feat(flutter): add onboarding screens (welcome, profile, health, wizard)"
```

---

## Task 10: Main App Screens

The four tabs: Home, Plan, History, Profile, plus the shared workout detail screen.

**Files:**
- Create: `run_planner_app/lib/screens/home/home_screen.dart`
- Create: `run_planner_app/lib/screens/plan/plan_screen.dart`
- Create: `run_planner_app/lib/screens/plan/workout_detail_screen.dart`
- Create: `run_planner_app/lib/screens/history/history_screen.dart`
- Create: `run_planner_app/lib/screens/profile/profile_screen.dart`
- Create: `run_planner_app/lib/screens/profile/edit_profile_screen.dart`

- [ ] **Step 1: Implement the home screen (dashboard)**

Create `run_planner_app/lib/screens/home/home_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/planned_workout.dart';
import '../../providers/plan_provider.dart';
import '../../providers/sync_provider.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/notification_banner.dart';
import '../../widgets/race_countdown_card.dart';
import '../../widgets/sync_indicator.dart';
import '../../widgets/week_day_indicators.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final planProvider = context.watch<PlanProvider>();
    final syncProvider = context.watch<SyncProvider>();
    final userProvider = context.watch<UserProvider>();

    final plan = planProvider.activePlan;
    final goalRace = planProvider.activeGoalRace;
    final todayWorkout = planProvider.todayWorkout;
    final flagged = userProvider.flaggedEntries;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Run Planner'),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: AppTheme.spacingMd),
            child: SyncIndicator(
              lastSyncedAt: syncProvider.lastSyncedAt,
              syncing: syncProvider.syncing,
              error: syncProvider.error,
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await Future.wait([
            planProvider.loadActivePlan(),
            syncProvider.sync(),
            userProvider.loadVdotHistory(),
          ]);
        },
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: AppTheme.spacingSm),
          children: [
            // Notification banner for flagged VDOT
            if (flagged.isNotEmpty)
              NotificationBanner(
                message:
                    'New fitness estimate: VDOT ${flagged.first.previousVdot.toStringAsFixed(1)} \u2192 ${flagged.first.newVdot.toStringAsFixed(1)}',
                actionLabel: 'Review',
                onTap: () {
                  // Navigate handled by router
                },
              ),

            // Race countdown
            if (goalRace != null && plan != null)
              RaceCountdownCard(
                raceName: goalRace.distanceLabel,
                raceDate: goalRace.raceDate.toIso8601String().split('T')[0],
                progressPercent: planProvider.progressPercent,
              ),

            // Today's workout
            _buildTodayCard(todayWorkout),

            // This week summary
            if (plan != null) _buildWeekSummary(planProvider),
          ],
        ),
      ),
    );
  }

  Widget _buildTodayCard(PlannedWorkoutResponse? workout) {
    final dayName = DateFormat('EEEE').format(DateTime.now());

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'TODAY \u00b7 $dayName',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                letterSpacing: 1,
                color: AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: AppTheme.spacingSm),
            if (workout == null)
              const Text(
                'No workout scheduled',
                style: TextStyle(fontSize: 16, color: AppTheme.textSecondary),
              )
            else if (workout.workoutType == WorkoutType.rest)
              const Text(
                'Rest Day',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              )
            else ...[
              Text(
                workout.workoutType.displayName,
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textPrimary,
                ),
              ),
              const SizedBox(height: AppTheme.spacingXs),
              Text(
                _workoutSubtitle(workout),
                style: const TextStyle(
                  fontSize: 15,
                  color: AppTheme.textSecondary,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildWeekSummary(PlanProvider planProvider) {
    final plan = planProvider.activePlan!;
    final now = DateTime.now();
    final weekNumber = planProvider.currentWeek;
    final totalWeeks = planProvider.totalWeeks;

    // Build day statuses from this week's workouts
    final weekStart = now.subtract(Duration(days: now.weekday - 1));
    final dayLabels = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

    final days = List.generate(7, (i) {
      final day = weekStart.add(Duration(days: i));
      final dayDate = DateTime(day.year, day.month, day.day);
      final isToday = dayDate.isAtSameMomentAs(
          DateTime(now.year, now.month, now.day));

      // Find planned workout for this day
      PlannedWorkoutResponse? pw;
      try {
        pw = plan.workouts.firstWhere(
            (w) => w.scheduledDate.isAtSameMomentAs(dayDate));
      } catch (_) {}

      return DayStatus(
        dayLabel: dayLabels[i],
        workoutType: pw?.workoutType ?? WorkoutType.rest,
        isToday: isToday,
        // Completed/missed status would come from workout matching data
        // For now, show past days as completed placeholder
        isCompleted: day.isBefore(now) && !isToday && pw != null,
      );
    });

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'This Week \u00b7 Week $weekNumber of $totalWeeks',
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                letterSpacing: 1,
                color: AppTheme.textSecondary,
              ),
            ),
            const SizedBox(height: AppTheme.spacingMd),
            WeekDayIndicators(days: days),
          ],
        ),
      ),
    );
  }

  String _workoutSubtitle(PlannedWorkoutResponse workout) {
    final km =
        (workout.targetDistanceMeters / 1000).toStringAsFixed(1);
    final parts = <String>[
      '$km km',
    ];
    if (workout.targetPaceMinPerKm != null &&
        workout.targetPaceMaxPerKm != null) {
      parts.add(
          '${_formatPace(workout.targetPaceMinPerKm!)}-${_formatPace(workout.targetPaceMaxPerKm!)}/km');
    }
    if (workout.targetHrZone != null) {
      parts.add('HR ${workout.targetHrZone} bpm');
    }
    return parts.join(' \u00b7 ');
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
```

- [ ] **Step 2: Implement the plan screen**

Create `run_planner_app/lib/screens/plan/plan_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/planned_workout.dart';
import '../../providers/plan_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/workout_card.dart';

class PlanScreen extends StatefulWidget {
  final void Function(PlannedWorkoutResponse workout)? onWorkoutTap;

  const PlanScreen({super.key, this.onWorkoutTap});

  @override
  State<PlanScreen> createState() => _PlanScreenState();
}

class _PlanScreenState extends State<PlanScreen> {
  late PageController _pageController;
  int _currentWeekOffset = 0;

  @override
  void initState() {
    super.initState();
    final planProvider = context.read<PlanProvider>();
    _currentWeekOffset = planProvider.currentWeek - 1;
    _pageController = PageController(initialPage: _currentWeekOffset);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final planProvider = context.watch<PlanProvider>();
    final plan = planProvider.activePlan;

    if (plan == null) {
      return const Scaffold(
        body: Center(child: Text('No active plan')),
      );
    }

    final totalWeeks = planProvider.totalWeeks;

    return Scaffold(
      appBar: AppBar(title: const Text('Training Plan')),
      body: Column(
        children: [
          // Week selector
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppTheme.spacingMd,
              vertical: AppTheme.spacingSm,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                IconButton(
                  icon: const Icon(Icons.chevron_left),
                  onPressed: _currentWeekOffset > 0
                      ? () {
                          _pageController.previousPage(
                            duration: const Duration(milliseconds: 300),
                            curve: Curves.easeInOut,
                          );
                        }
                      : null,
                ),
                Column(
                  children: [
                    Text(
                      'Week ${_currentWeekOffset + 1} of $totalWeeks',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      _weekDateRange(_currentWeekOffset, plan.startDate),
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
                IconButton(
                  icon: const Icon(Icons.chevron_right),
                  onPressed: _currentWeekOffset < totalWeeks - 1
                      ? () {
                          _pageController.nextPage(
                            duration: const Duration(milliseconds: 300),
                            curve: Curves.easeInOut,
                          );
                        }
                      : null,
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          // Week pages
          Expanded(
            child: PageView.builder(
              controller: _pageController,
              itemCount: totalWeeks,
              onPageChanged: (index) {
                setState(() => _currentWeekOffset = index);
              },
              itemBuilder: (context, weekIndex) {
                return _buildWeekPage(weekIndex, plan);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWeekPage(
      int weekIndex, dynamic plan) {
    final weekNumber = weekIndex + 1;
    final weekWorkouts = (plan.workouts as List<PlannedWorkoutResponse>)
        .where((w) => w.weekNumber == weekNumber)
        .toList()
      ..sort((a, b) => a.dayOfWeek.compareTo(b.dayOfWeek));

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final dayLabels = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

    return ListView.builder(
      itemCount: weekWorkouts.length,
      itemBuilder: (context, index) {
        final workout = weekWorkouts[index];
        final isToday = workout.scheduledDate.isAtSameMomentAs(today);
        final isMissed = workout.scheduledDate.isBefore(today) &&
            workout.workoutType != WorkoutType.rest;
        final dayName = dayLabels[(workout.dayOfWeek - 1) % 7];
        final dateStr = DateFormat('MMM d').format(workout.scheduledDate);

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(
                left: AppTheme.spacingMd,
                top: AppTheme.spacingSm,
                bottom: AppTheme.spacingXs,
              ),
              child: Text(
                '$dayName \u00b7 $dateStr',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: isToday ? AppTheme.primary : AppTheme.textSecondary,
                ),
              ),
            ),
            WorkoutCard(
              workout: workout,
              isToday: isToday,
              isMissed: isMissed,
              onTap: () => widget.onWorkoutTap?.call(workout),
            ),
          ],
        );
      },
    );
  }

  String _weekDateRange(int weekOffset, DateTime planStart) {
    final weekStart = planStart.add(Duration(days: weekOffset * 7));
    final weekEnd = weekStart.add(const Duration(days: 6));
    return '${DateFormat('MMM d').format(weekStart)} - ${DateFormat('MMM d').format(weekEnd)}';
  }
}
```

- [ ] **Step 3: Implement the workout detail screen**

Create `run_planner_app/lib/screens/plan/workout_detail_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../models/planned_workout.dart';
import '../../models/workout.dart';
import '../../theme/app_theme.dart';
import '../../widgets/compliance_bars.dart';

class WorkoutDetailScreen extends StatelessWidget {
  final PlannedWorkoutResponse? plannedWorkout;
  final WorkoutResponse? actualWorkout;
  final double? complianceScore;
  final double? distanceScore;
  final double? paceScore;
  final double? hrScore;

  const WorkoutDetailScreen({
    super.key,
    this.plannedWorkout,
    this.actualWorkout,
    this.complianceScore,
    this.distanceScore,
    this.paceScore,
    this.hrScore,
  });

  @override
  Widget build(BuildContext context) {
    final title = plannedWorkout?.workoutType.displayName ??
        'Workout';
    final date = plannedWorkout?.scheduledDate ?? actualWorkout?.startedAt;

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppTheme.spacingMd),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (date != null)
              Text(
                DateFormat('EEEE, MMM d').format(date),
                style: const TextStyle(
                  fontSize: 16,
                  color: AppTheme.textSecondary,
                ),
              ),
            const SizedBox(height: AppTheme.spacingLg),

            // Planned section
            if (plannedWorkout != null) ...[
              _buildSectionHeader('PLANNED'),
              const SizedBox(height: AppTheme.spacingSm),
              _buildRow('Distance',
                  '${(plannedWorkout!.targetDistanceMeters / 1000).toStringAsFixed(1)} km'),
              if (plannedWorkout!.targetPaceMinPerKm != null)
                _buildRow('Pace',
                    '${_formatPace(plannedWorkout!.targetPaceMinPerKm!)} - ${_formatPace(plannedWorkout!.targetPaceMaxPerKm!)}/km'),
              if (plannedWorkout!.targetHrZone != null)
                _buildRow('HR Zone', '${plannedWorkout!.targetHrZone} bpm'),
              const SizedBox(height: AppTheme.spacingLg),
            ],

            // Actual section
            if (actualWorkout != null) ...[
              _buildSectionHeader('ACTUAL'),
              const SizedBox(height: AppTheme.spacingSm),
              _buildRow('Distance',
                  '${(actualWorkout!.distanceMeters / 1000).toStringAsFixed(2)} km'),
              _buildRow('Pace',
                  '${_formatPace(actualWorkout!.paceMinPerKm)}/km'),
              if (actualWorkout!.avgHr != null)
                _buildRow('Avg HR', '${actualWorkout!.avgHr} bpm'),
              if (actualWorkout!.maxHr != null)
                _buildRow('Max HR', '${actualWorkout!.maxHr} bpm'),
              const SizedBox(height: AppTheme.spacingLg),
            ],

            // Compliance bars
            if (complianceScore != null)
              ComplianceBars(
                overallScore: complianceScore!,
                distanceScore: distanceScore ?? 0,
                paceScore: paceScore ?? 0,
                hrScore: hrScore ?? 0,
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        letterSpacing: 1,
        color: AppTheme.textSecondary,
      ),
    );
  }

  Widget _buildRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppTheme.spacingSm),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label,
              style: const TextStyle(
                  fontSize: 15, color: AppTheme.textSecondary)),
          Text(value,
              style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                  color: AppTheme.textPrimary)),
        ],
      ),
    );
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
```

- [ ] **Step 4: Implement the history screen**

Create `run_planner_app/lib/screens/history/history_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../models/workout.dart';
import '../../providers/workout_provider.dart';
import '../../theme/app_theme.dart';

class HistoryScreen extends StatefulWidget {
  final void Function(WorkoutResponse workout)? onWorkoutTap;

  const HistoryScreen({super.key, this.onWorkoutTap});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  @override
  void initState() {
    super.initState();
    context.read<WorkoutProvider>().loadWorkouts();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<WorkoutProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('History')),
      body: Column(
        children: [
          // Filter toggles
          Padding(
            padding: const EdgeInsets.all(AppTheme.spacingSm),
            child: SegmentedButton<WorkoutFilter>(
              segments: const [
                ButtonSegment(value: WorkoutFilter.all, label: Text('All')),
                ButtonSegment(
                    value: WorkoutFilter.matched, label: Text('Matched')),
                ButtonSegment(
                    value: WorkoutFilter.flagged, label: Text('Flagged')),
              ],
              selected: {provider.filter},
              onSelectionChanged: (v) => provider.setFilter(v.first),
            ),
          ),
          const Divider(height: 1),
          // Workout list
          Expanded(
            child: provider.loading
                ? const Center(child: CircularProgressIndicator())
                : provider.workouts.isEmpty
                    ? const Center(
                        child: Text(
                          'No workouts yet',
                          style: TextStyle(color: AppTheme.textSecondary),
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: () => provider.loadWorkouts(),
                        child: _buildWorkoutList(provider.workouts),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildWorkoutList(List<WorkoutResponse> workouts) {
    // Group by date
    final grouped = <String, List<WorkoutResponse>>{};
    for (final w in workouts) {
      final key = DateFormat('yyyy-MM-dd').format(w.startedAt);
      grouped.putIfAbsent(key, () => []).add(w);
    }

    final sortedKeys = grouped.keys.toList()..sort((a, b) => b.compareTo(a));

    return ListView.builder(
      itemCount: sortedKeys.length,
      itemBuilder: (context, index) {
        final dateKey = sortedKeys[index];
        final dayWorkouts = grouped[dateKey]!;
        final date = DateTime.parse(dateKey);

        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final yesterday = today.subtract(const Duration(days: 1));
        String dateLabel;
        if (date.isAtSameMomentAs(today)) {
          dateLabel = 'Today';
        } else if (date.isAtSameMomentAs(yesterday)) {
          dateLabel = 'Yesterday';
        } else {
          dateLabel = DateFormat('MMM d').format(date);
        }

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(
                left: AppTheme.spacingMd,
                top: AppTheme.spacingMd,
                bottom: AppTheme.spacingXs,
              ),
              child: Text(
                dateLabel,
                style: const TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: AppTheme.textSecondary,
                ),
              ),
            ),
            ...dayWorkouts.map((w) => _buildWorkoutRow(w)),
          ],
        );
      },
    );
  }

  Widget _buildWorkoutRow(WorkoutResponse workout) {
    final km = (workout.distanceMeters / 1000).toStringAsFixed(2);
    final pace = _formatPace(workout.paceMinPerKm);

    return InkWell(
      onTap: () => widget.onWorkoutTap?.call(workout),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spacingMd,
          vertical: AppTheme.spacingSm,
        ),
        decoration: const BoxDecoration(
          border: Border(
            bottom: BorderSide(color: AppTheme.divider, width: 0.5),
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$km km \u00b7 $pace/km',
                    style: const TextStyle(
                      fontSize: 16,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  if (workout.avgHr != null)
                    Text(
                      '${workout.avgHr} bpm avg',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right,
                color: AppTheme.textSecondary, size: 20),
          ],
        ),
      ),
    );
  }

  String _formatPace(double minPerKm) {
    final minutes = minPerKm.floor();
    final seconds = ((minPerKm - minutes) * 60).round();
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }
}
```

- [ ] **Step 5: Implement the profile screen**

Create `run_planner_app/lib/screens/profile/profile_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/plan_provider.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/vdot_chart.dart';

class ProfileScreen extends StatefulWidget {
  final VoidCallback? onEditProfile;
  final VoidCallback? onNewGoalRace;
  final VoidCallback? onLogout;

  const ProfileScreen({
    super.key,
    this.onEditProfile,
    this.onNewGoalRace,
    this.onLogout,
  });

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  @override
  void initState() {
    super.initState();
    context.read<UserProvider>().loadVdotHistory();
    context.read<PlanProvider>().loadGoalRaces();
  }

  @override
  Widget build(BuildContext context) {
    final userProvider = context.watch<UserProvider>();
    final planProvider = context.watch<PlanProvider>();
    final user = userProvider.user;

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: ListView(
        children: [
          // User info
          Card(
            child: InkWell(
              onTap: widget.onEditProfile,
              child: Padding(
                padding: const EdgeInsets.all(AppTheme.spacingMd),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            user?.name ?? user?.email ?? '',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              color: AppTheme.textPrimary,
                            ),
                          ),
                          const SizedBox(height: AppTheme.spacingXs),
                          Text(
                            [
                              if (userProvider.currentVdot != null)
                                'VDOT: ${userProvider.currentVdot!.toStringAsFixed(1)}',
                              'Units: ${user?.preferredUnits.name ?? 'metric'}',
                              if (user?.maxHr != null)
                                'HR max: ${user!.maxHr}',
                            ].join(' \u00b7 '),
                            style: const TextStyle(
                              fontSize: 14,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Icon(Icons.chevron_right,
                        color: AppTheme.textSecondary),
                  ],
                ),
              ),
            ),
          ),

          // VDOT History chart
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacingMd),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Fitness Over Time',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: AppTheme.textPrimary,
                    ),
                  ),
                  const SizedBox(height: AppTheme.spacingSm),
                  if (userProvider.currentVdot != null)
                    Text(
                      'Current: ${userProvider.currentVdot!.toStringAsFixed(1)}',
                      style: const TextStyle(
                        fontSize: 14,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                  const SizedBox(height: AppTheme.spacingMd),
                  VdotChart(history: userProvider.vdotHistory),
                ],
              ),
            ),
          ),

          // Goal races
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacingMd),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Goal Races',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                      TextButton(
                        onPressed: widget.onNewGoalRace,
                        child: const Text('New Race'),
                      ),
                    ],
                  ),
                  ...planProvider.goalRaces.map((race) => Padding(
                        padding: const EdgeInsets.only(
                            bottom: AppTheme.spacingSm),
                        child: Row(
                          mainAxisAlignment:
                              MainAxisAlignment.spaceBetween,
                          children: [
                            Text(race.distanceLabel),
                            Text(
                              race.status.name.toUpperCase(),
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppTheme.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      )),
                ],
              ),
            ),
          ),

          // Settings
          Card(
            child: Column(
              children: [
                ListTile(
                  title: const Text('Log out'),
                  leading:
                      const Icon(Icons.logout, color: AppTheme.error),
                  onTap: () async {
                    await context.read<AuthProvider>().logout();
                    widget.onLogout?.call();
                  },
                ),
              ],
            ),
          ),

          const SizedBox(height: AppTheme.spacingXl),
        ],
      ),
    );
  }
}
```

- [ ] **Step 6: Implement the edit profile screen**

Create `run_planner_app/lib/screens/profile/edit_profile_screen.dart`:

```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/user.dart';
import '../../providers/user_provider.dart';
import '../../theme/app_theme.dart';

class EditProfileScreen extends StatefulWidget {
  final VoidCallback? onSaved;

  const EditProfileScreen({super.key, this.onSaved});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  late TextEditingController _nameController;
  late TextEditingController _maxHrController;
  DateTime? _dateOfBirth;
  late Units _units;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    final user = context.read<UserProvider>().user;
    _nameController = TextEditingController(text: user?.name ?? '');
    _maxHrController =
        TextEditingController(text: user?.maxHr?.toString() ?? '');
    _dateOfBirth = user?.dateOfBirth;
    _units = user?.preferredUnits ?? Units.metric;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _maxHrController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    setState(() => _loading = true);
    try {
      await context.read<UserProvider>().updateProfile(
            UpdateProfileRequest(
              name: _nameController.text.trim(),
              dateOfBirth: _dateOfBirth,
              maxHr: int.tryParse(_maxHrController.text),
              preferredUnits: _units,
            ),
          );
      widget.onSaved?.call();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Edit Profile'),
        actions: [
          TextButton(
            onPressed: _loading ? null : _save,
            child: _loading
                ? const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Save'),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppTheme.spacingLg),
        child: Column(
          children: [
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'Name'),
            ),
            const SizedBox(height: AppTheme.spacingMd),
            TextFormField(
              controller: _maxHrController,
              decoration:
                  const InputDecoration(labelText: 'Max Heart Rate'),
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: AppTheme.spacingMd),
            const Text('Preferred Units',
                style: TextStyle(color: AppTheme.textSecondary)),
            const SizedBox(height: AppTheme.spacingSm),
            SegmentedButton<Units>(
              segments: const [
                ButtonSegment(
                    value: Units.metric, label: Text('Kilometers')),
                ButtonSegment(
                    value: Units.imperial, label: Text('Miles')),
              ],
              selected: {_units},
              onSelectionChanged: (v) =>
                  setState(() => _units = v.first),
            ),
          ],
        ),
      ),
    );
  }
}
```

- [ ] **Step 7: Verify compilation**

Run: `cd run_planner_app && flutter analyze`

Expected: "No issues found!"

- [ ] **Step 8: Commit**

```bash
git add run_planner_app/lib/screens/
git commit -m "feat(flutter): add all main app screens (home, plan, history, profile)"
```

---

## Task 11: Router and Main App Wiring

Connect everything together with go_router navigation and Provider wiring in main.dart.

**Files:**
- Modify: `run_planner_app/lib/main.dart`

- [ ] **Step 1: Wire up main.dart with providers and routing**

Replace `run_planner_app/lib/main.dart` with:

```dart
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import 'providers/auth_provider.dart';
import 'providers/plan_provider.dart';
import 'providers/sync_provider.dart';
import 'providers/user_provider.dart';
import 'providers/workout_provider.dart';
import 'screens/history/history_screen.dart';
import 'screens/home/home_screen.dart';
import 'screens/onboarding/goal_race_wizard_screen.dart';
import 'screens/onboarding/health_permission_screen.dart';
import 'screens/onboarding/initial_sync_screen.dart';
import 'screens/onboarding/profile_setup_screen.dart';
import 'screens/onboarding/welcome_screen.dart';
import 'screens/plan/plan_screen.dart';
import 'screens/plan/workout_detail_screen.dart';
import 'screens/profile/edit_profile_screen.dart';
import 'screens/profile/profile_screen.dart';
import 'services/api_client.dart';
import 'services/auth_service.dart';
import 'services/goal_race_service.dart';
import 'services/health_kit_service.dart';
import 'services/health_sync_service.dart';
import 'services/plan_service.dart';
import 'services/user_service.dart';
import 'services/vdot_service.dart';
import 'services/workout_service.dart';
import 'theme/app_theme.dart';

/// Adapts FlutterSecureStorage to our TokenStorage interface.
class SecureTokenStorage implements TokenStorage {
  final _storage = const FlutterSecureStorage();

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) =>
      _storage.write(key: key, value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);
}

void main() {
  runApp(const RunPlannerApp());
}

class RunPlannerApp extends StatefulWidget {
  const RunPlannerApp({super.key});

  @override
  State<RunPlannerApp> createState() => _RunPlannerAppState();
}

class _RunPlannerAppState extends State<RunPlannerApp> {
  late final AuthProvider _authProvider;
  late final ApiClient _apiClient;
  late final GoRouter _router;

  @override
  void initState() {
    super.initState();

    // Build the service layer
    final httpClient = http.Client();
    final tokenStorage = SecureTokenStorage();

    // ApiClient for unauthenticated calls (login, register)
    final publicApiClient = ApiClient(
      httpClient: httpClient,
      getToken: () => null,
    );

    final authService = AuthService(apiClient: publicApiClient);
    _authProvider = AuthProvider(
      authService: authService,
      tokenStorage: tokenStorage,
    );

    // ApiClient for authenticated calls — reads token from AuthProvider
    _apiClient = ApiClient(
      httpClient: httpClient,
      getToken: () => _authProvider.accessToken,
      onUnauthorized: () async {
        final refreshed = await _authProvider.tryRefreshToken();
        if (!refreshed) {
          _router.go('/welcome');
        }
      },
    );

    _router = _buildRouter();

    // Try restoring session on startup
    _authProvider.tryRestoreSession();
  }

  GoRouter _buildRouter() {
    return GoRouter(
      initialLocation: '/welcome',
      redirect: (context, state) {
        final loggedIn = _authProvider.isAuthenticated;
        final onAuthPage = state.matchedLocation == '/welcome';

        if (!loggedIn && !onAuthPage) return '/welcome';
        return null;
      },
      routes: [
        GoRoute(
          path: '/welcome',
          builder: (context, state) => WelcomeScreen(
            onAuthenticated: () => context.go('/profile-setup'),
          ),
        ),
        GoRoute(
          path: '/profile-setup',
          builder: (context, state) => ProfileSetupScreen(
            onComplete: () => context.go('/health-permission'),
          ),
        ),
        GoRoute(
          path: '/health-permission',
          builder: (context, state) => HealthPermissionScreen(
            onComplete: () => context.go('/initial-sync'),
          ),
        ),
        GoRoute(
          path: '/initial-sync',
          builder: (context, state) => InitialSyncScreen(
            onComplete: () => context.go('/goal-race-wizard'),
          ),
        ),
        GoRoute(
          path: '/goal-race-wizard',
          builder: (context, state) => GoalRaceWizardScreen(
            onComplete: () => context.go('/'),
          ),
        ),
        // Main app shell with bottom tabs
        ShellRoute(
          builder: (context, state, child) {
            return _MainShell(child: child);
          },
          routes: [
            GoRoute(
              path: '/',
              builder: (context, state) => const HomeScreen(),
            ),
            GoRoute(
              path: '/plan',
              builder: (context, state) => PlanScreen(
                onWorkoutTap: (workout) => context.push('/workout-detail'),
              ),
            ),
            GoRoute(
              path: '/history',
              builder: (context, state) => HistoryScreen(
                onWorkoutTap: (workout) => context.push('/workout-detail'),
              ),
            ),
            GoRoute(
              path: '/profile',
              builder: (context, state) => ProfileScreen(
                onEditProfile: () => context.push('/edit-profile'),
                onNewGoalRace: () => context.push('/goal-race-wizard'),
                onLogout: () => context.go('/welcome'),
              ),
            ),
          ],
        ),
        GoRoute(
          path: '/workout-detail',
          builder: (context, state) => const WorkoutDetailScreen(),
        ),
        GoRoute(
          path: '/edit-profile',
          builder: (context, state) => EditProfileScreen(
            onSaved: () => context.pop(),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: _authProvider),
        ChangeNotifierProvider(
          create: (_) => UserProvider(
            userService: UserService(apiClient: _apiClient),
            vdotService: VdotService(apiClient: _apiClient),
          ),
        ),
        ChangeNotifierProvider(
          create: (_) => PlanProvider(
            planService: PlanService(apiClient: _apiClient),
            goalRaceService: GoalRaceService(apiClient: _apiClient),
          ),
        ),
        ChangeNotifierProvider(
          create: (_) =>
              WorkoutProvider(workoutService: WorkoutService(apiClient: _apiClient)),
        ),
        ChangeNotifierProvider(
          create: (_) => SyncProvider(
            healthKitService: HealthKitService(),
            healthSyncService: HealthSyncService(apiClient: _apiClient),
          ),
        ),
      ],
      child: MaterialApp.router(
        title: 'Run Planner',
        theme: AppTheme.lightTheme,
        routerConfig: _router,
      ),
    );
  }
}

class _MainShell extends StatelessWidget {
  final Widget child;

  const _MainShell({required this.child});

  @override
  Widget build(BuildContext context) {
    // Determine selected index from current route
    final location = GoRouterState.of(context).matchedLocation;
    int currentIndex = 0;
    if (location == '/plan') currentIndex = 1;
    if (location == '/history') currentIndex = 2;
    if (location == '/profile') currentIndex = 3;

    return Scaffold(
      body: child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: currentIndex,
        onDestinationSelected: (index) {
          switch (index) {
            case 0:
              context.go('/');
            case 1:
              context.go('/plan');
            case 2:
              context.go('/history');
            case 3:
              context.go('/profile');
          }
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(
              icon: Icon(Icons.calendar_month), label: 'Plan'),
          NavigationDestination(
              icon: Icon(Icons.history), label: 'History'),
          NavigationDestination(icon: Icon(Icons.person), label: 'Profile'),
        ],
      ),
    );
  }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd run_planner_app && flutter analyze`

Expected: "No issues found!"

- [ ] **Step 3: Run all tests**

Run: `cd run_planner_app && flutter test`

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
git add run_planner_app/lib/main.dart
git commit -m "feat(flutter): wire up router, providers, and main app shell"
```

---

## Task 12: iOS Configuration

Configure iOS-specific settings for HealthKit permissions and app metadata.

**Files:**
- Modify: `run_planner_app/ios/Runner/Info.plist`
- Modify: `run_planner_app/ios/Runner.xcodeproj/project.pbxproj` (via Xcode or manual edit)

- [ ] **Step 1: Add HealthKit usage descriptions to Info.plist**

Add the following keys inside the `<dict>` section of `run_planner_app/ios/Runner/Info.plist`:

```xml
<key>NSHealthShareUsageDescription</key>
<string>Run Planner reads your workout history, heart rate, and VO2 max to calculate your fitness level and generate personalized training plans.</string>
<key>NSHealthUpdateUsageDescription</key>
<string>Run Planner does not write to Apple Health.</string>
```

- [ ] **Step 2: Enable HealthKit capability**

This step requires Xcode. Open `run_planner_app/ios/Runner.xcworkspace` in Xcode:
1. Select the Runner target
2. Go to "Signing & Capabilities"
3. Click "+ Capability" and add "HealthKit"

Alternatively, add to `run_planner_app/ios/Runner/Runner.entitlements`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.developer.healthkit</key>
    <true/>
    <key>com.apple.developer.healthkit.access</key>
    <array/>
</dict>
</plist>
```

- [ ] **Step 3: Verify iOS build**

Run: `cd run_planner_app && flutter build ios --no-codesign`

Expected: Build succeeds (may show warnings but no errors).

- [ ] **Step 4: Commit**

```bash
git add run_planner_app/ios/
git commit -m "feat(flutter): configure iOS HealthKit permissions and capability"
```

---

## Task 13: End-to-End Smoke Test

Verify the full app compiles, tests pass, and the app runs on the iOS simulator.

**Files:** None — verification only.

- [ ] **Step 1: Run all tests**

Run: `cd run_planner_app && flutter test`

Expected: All tests pass.

- [ ] **Step 2: Run static analysis**

Run: `cd run_planner_app && flutter analyze`

Expected: "No issues found!" (or only minor warnings).

- [ ] **Step 3: Launch on iOS simulator**

Run: `cd run_planner_app && flutter run`

Expected: The app launches on the iOS simulator showing the Welcome screen with login/register form. Verify:
- Welcome screen renders with email/password fields
- Toggle between "Log In" and "Create Account" works
- App bar and theme colors match design (blue primary, clean white background)

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat(flutter): complete v1 Flutter frontend implementation"
```
