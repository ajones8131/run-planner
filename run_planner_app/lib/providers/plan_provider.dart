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

  PlanProvider({required PlanService planService, required GoalRaceService goalRaceService})
      : _planService = planService,
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
    return _activePlan!.endDate.difference(_activePlan!.startDate).inDays ~/ 7;
  }

  int get currentWeek {
    if (_activePlan == null) return 0;
    final daysSinceStart = DateTime.now().difference(_activePlan!.startDate).inDays;
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
      return _activePlan?.workouts.firstWhere((w) => w.scheduledDate.isAtSameMomentAs(today));
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
      _error = null;
    }
    _loading = false;
    notifyListeners();
  }

  Future<void> loadWeekWorkouts(String planId, DateTime weekStart) async {
    try {
      final weekEnd = weekStart.add(const Duration(days: 6));
      _weekWorkouts = await _planService.getWorkouts(planId, from: weekStart, to: weekEnd);
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

  Future<GoalRaceResponse> createGoalRace(CreateGoalRaceRequest request) async {
    final race = await _goalRaceService.create(request);
    await loadGoalRaces();
    return race;
  }

  Future<TrainingPlanResponse> createPlan(String goalRaceId) async {
    final plan = await _planService.create(CreatePlanRequest(goalRaceId: goalRaceId));
    _activePlan = plan;
    notifyListeners();
    return plan;
  }

  Future<void> _loadActiveGoalRace() async {
    if (_activePlan == null) return;
    await loadGoalRaces();
    try {
      _activeGoalRace = _goalRaces.firstWhere((r) => r.id == _activePlan!.goalRaceId);
    } catch (_) {
      _activeGoalRace = null;
    }
  }
}
