import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import 'models/planned_workout.dart';
import 'models/workout.dart';
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
/// Used on iOS where Keychain is available.
class SecureTokenStorage implements TokenStorage {
  final _storage = const FlutterSecureStorage();

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) => _storage.write(key: key, value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);
}

/// In-memory token storage for platforms where Keychain isn't available (macOS debug, web).
/// Tokens won't persist across app restarts, but that's fine for development.
class InMemoryTokenStorage implements TokenStorage {
  final Map<String, String> _store = {};

  @override
  Future<String?> read(String key) async => _store[key];

  @override
  Future<void> write(String key, String value) async => _store[key] = value;

  @override
  Future<void> delete(String key) async => _store.remove(key);
}

TokenStorage _createTokenStorage() {
  if (kIsWeb) return InMemoryTokenStorage();
  if (Platform.isIOS) return SecureTokenStorage();
  return InMemoryTokenStorage();
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
    final httpClient = http.Client();
    final tokenStorage = _createTokenStorage();

    // Public API client (no auth header) for login/register
    final publicApiClient = ApiClient(httpClient: httpClient, getToken: () => null);
    final authService = AuthService(apiClient: publicApiClient);
    _authProvider = AuthProvider(authService: authService, tokenStorage: tokenStorage);

    // Authenticated API client — reads token from AuthProvider
    _apiClient = ApiClient(
      httpClient: httpClient,
      getToken: () => _authProvider.accessToken,
      onUnauthorized: () async {
        final refreshed = await _authProvider.tryRefreshToken();
        if (!refreshed) _router.go('/welcome');
      },
    );

    _router = _buildRouter();
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
        GoRoute(path: '/welcome', builder: (context, state) {
          final planProvider = context.read<PlanProvider>();
          return WelcomeScreen(
            onAuthenticated: () async {
              // Check if returning user with active plan
              try {
                await planProvider.loadActivePlan();
                if (planProvider.hasActivePlan) {
                  _router.go('/');
                  return;
                }
              } catch (_) {}
              // New user or no plan — start onboarding
              _router.go('/profile-setup');
            },
          );
        }),
        GoRoute(path: '/profile-setup', builder: (context, state) => ProfileSetupScreen(onComplete: () => context.go('/health-permission'))),
        GoRoute(path: '/health-permission', builder: (context, state) => HealthPermissionScreen(onComplete: () => context.go('/initial-sync'))),
        GoRoute(path: '/initial-sync', builder: (context, state) => InitialSyncScreen(onComplete: () => context.go('/goal-race-wizard'))),
        GoRoute(path: '/goal-race-wizard', builder: (context, state) => GoalRaceWizardScreen(onComplete: () => context.go('/'))),
        // Main app shell with bottom tabs
        ShellRoute(
          builder: (context, state, child) => _MainShell(child: child),
          routes: [
            GoRoute(path: '/', builder: (context, state) => const HomeScreen()),
            GoRoute(path: '/plan', builder: (context, state) => PlanScreen(onWorkoutTap: (workout) => context.push('/workout-detail', extra: workout))),
            GoRoute(path: '/history', builder: (context, state) => HistoryScreen(onWorkoutTap: (workout) => context.push('/workout-detail', extra: workout))),
            GoRoute(path: '/profile', builder: (context, state) => ProfileScreen(
              onEditProfile: () => context.push('/edit-profile'),
              onNewGoalRace: () => context.push('/goal-race-wizard'),
              onLogout: () => context.go('/welcome'),
            )),
          ],
        ),
        GoRoute(path: '/workout-detail', builder: (context, state) {
          final extra = state.extra;
          if (extra is PlannedWorkoutResponse) {
            return WorkoutDetailScreen(plannedWorkout: extra);
          }
          if (extra is WorkoutResponse) {
            return WorkoutDetailScreen(actualWorkout: extra);
          }
          return const WorkoutDetailScreen();
        }),
        GoRoute(path: '/edit-profile', builder: (context, state) => EditProfileScreen(onSaved: () => context.pop())),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: _authProvider),
        ChangeNotifierProvider(create: (_) => UserProvider(userService: UserService(apiClient: _apiClient), vdotService: VdotService(apiClient: _apiClient))),
        ChangeNotifierProvider(create: (_) => PlanProvider(planService: PlanService(apiClient: _apiClient), goalRaceService: GoalRaceService(apiClient: _apiClient))),
        ChangeNotifierProvider(create: (_) => WorkoutProvider(workoutService: WorkoutService(apiClient: _apiClient))),
        ChangeNotifierProvider(create: (_) => SyncProvider(healthKitService: HealthKitService(), healthSyncService: HealthSyncService(apiClient: _apiClient))),
      ],
      child: MaterialApp.router(title: 'Run Planner', theme: AppTheme.lightTheme, routerConfig: _router),
    );
  }
}

class _MainShell extends StatelessWidget {
  final Widget child;
  const _MainShell({required this.child});

  @override
  Widget build(BuildContext context) {
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
            case 0: context.go('/');
            case 1: context.go('/plan');
            case 2: context.go('/history');
            case 3: context.go('/profile');
          }
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.calendar_month), label: 'Plan'),
          NavigationDestination(icon: Icon(Icons.history), label: 'History'),
          NavigationDestination(icon: Icon(Icons.person), label: 'Profile'),
        ],
      ),
    );
  }
}
