import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/training_plan.dart';

void main() {
  group('TrainingPlanResponse', () {
    final workoutJson = {
      'id': 'workout-1',
      'weekNumber': 1,
      'dayOfWeek': 1,
      'scheduledDate': '2026-04-07',
      'workoutType': 'EASY',
      'targetDistanceMeters': 8000.0,
      'targetPaceMinPerKm': null,
      'targetPaceMaxPerKm': null,
      'targetHrZone': null,
      'notes': null,
      'planRevision': 1,
    };

    test('fromJson parses all fields including workouts', () {
      final json = {
        'id': 'plan-123',
        'goalRaceId': 'race-456',
        'startDate': '2026-04-06',
        'endDate': '2026-10-11',
        'status': 'ACTIVE',
        'revision': 1,
        'createdAt': '2026-03-25T10:00:00.000Z',
        'workouts': [workoutJson],
      };

      final response = TrainingPlanResponse.fromJson(json);

      expect(response.id, equals('plan-123'));
      expect(response.goalRaceId, equals('race-456'));
      expect(response.startDate, equals(DateTime(2026, 4, 6)));
      expect(response.endDate, equals(DateTime(2026, 10, 11)));
      expect(response.status, equals(TrainingPlanStatus.active));
      expect(response.revision, equals(1));
      expect(response.workouts.length, equals(1));
      expect(response.workouts.first.id, equals('workout-1'));
    });

    test('fromJson parses empty workouts list', () {
      final json = {
        'id': 'plan-789',
        'goalRaceId': 'race-456',
        'startDate': '2026-04-06',
        'endDate': '2026-10-11',
        'status': 'ACTIVE',
        'revision': 2,
        'createdAt': '2026-03-25T10:00:00.000Z',
        'workouts': <dynamic>[],
      };

      final response = TrainingPlanResponse.fromJson(json);

      expect(response.workouts, isEmpty);
    });

    test('fromJson handles null workouts as empty list', () {
      final json = {
        'id': 'plan-null',
        'goalRaceId': 'race-456',
        'startDate': '2026-04-06',
        'endDate': '2026-10-11',
        'status': 'COMPLETED',
        'revision': 3,
        'createdAt': '2026-03-25T10:00:00.000Z',
        'workouts': null,
      };

      final response = TrainingPlanResponse.fromJson(json);

      expect(response.workouts, isEmpty);
    });
  });

  group('CreatePlanRequest', () {
    test('toJson includes goalRaceId', () {
      const request = CreatePlanRequest(goalRaceId: 'race-123');

      final json = request.toJson();

      expect(json['goalRaceId'], equals('race-123'));
    });
  });
}
