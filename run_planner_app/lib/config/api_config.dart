class ApiConfig {
  ApiConfig._();

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
