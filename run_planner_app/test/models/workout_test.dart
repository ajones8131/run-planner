import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/workout.dart';

void main() {
  group('WorkoutResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'workout-123',
        'startedAt': '2026-03-20T07:30:00.000Z',
        'distanceMeters': 10000.0,
        'durationSeconds': 3000,
        'avgHr': 155,
        'maxHr': 172,
        'elevationGain': 120.5,
        'source': 'APPLE_HEALTH',
        'sourceId': 'external-id-abc',
      };

      final response = WorkoutResponse.fromJson(json);

      expect(response.id, equals('workout-123'));
      expect(
        response.startedAt,
        equals(DateTime.utc(2026, 3, 20, 7, 30, 0)),
      );
      expect(response.distanceMeters, equals(10000.0));
      expect(response.durationSeconds, equals(3000));
      expect(response.avgHr, equals(155));
      expect(response.maxHr, equals(172));
      expect(response.elevationGain, equals(120.5));
      expect(response.source, equals('APPLE_HEALTH'));
      expect(response.sourceId, equals('external-id-abc'));
    });

    test('fromJson handles null optional fields', () {
      final json = {
        'id': 'workout-456',
        'startedAt': '2026-03-21T06:00:00.000Z',
        'distanceMeters': 5000.0,
        'durationSeconds': 1500,
        'avgHr': null,
        'maxHr': null,
        'elevationGain': null,
        'source': 'MANUAL',
        'sourceId': 'manual-001',
      };

      final response = WorkoutResponse.fromJson(json);

      expect(response.avgHr, isNull);
      expect(response.maxHr, isNull);
      expect(response.elevationGain, isNull);
    });

    group('paceMinPerKm', () {
      test('calculates pace correctly', () {
        // 10km in 50 minutes = 5.0 min/km
        final workout = WorkoutResponse(
          id: 'w',
          startedAt: DateTime.now(),
          distanceMeters: 10000,
          durationSeconds: 3000,
          source: 'APPLE_HEALTH',
          sourceId: 'x',
        );

        expect(workout.paceMinPerKm, closeTo(5.0, 0.001));
      });

      test('returns 0 when distanceMeters is 0', () {
        final workout = WorkoutResponse(
          id: 'w',
          startedAt: DateTime.now(),
          distanceMeters: 0,
          durationSeconds: 600,
          source: 'APPLE_HEALTH',
          sourceId: 'x',
        );

        expect(workout.paceMinPerKm, equals(0.0));
      });
    });
  });
}
