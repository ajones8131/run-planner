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

  WorkoutProvider({required WorkoutService workoutService}) : _workoutService = workoutService;

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
