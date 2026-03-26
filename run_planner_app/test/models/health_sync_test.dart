import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/health_sync.dart';

void main() {
  group('WorkoutSyncItem', () {
    test('toJson includes all optional fields when set', () {
      final item = WorkoutSyncItem(
        source: 'APPLE_HEALTH',
        sourceId: 'ext-123',
        startedAt: DateTime.utc(2026, 3, 20, 7, 0, 0),
        distanceMeters: 10000.0,
        durationSeconds: 3000,
        avgHr: 150,
        maxHr: 170,
        elevationGain: 50.0,
      );

      final json = item.toJson();

      expect(json['source'], equals('APPLE_HEALTH'));
      expect(json['sourceId'], equals('ext-123'));
      expect(json['distanceMeters'], equals(10000.0));
      expect(json['durationSeconds'], equals(3000));
      expect(json['avgHr'], equals(150));
      expect(json['maxHr'], equals(170));
      expect(json['elevationGain'], equals(50.0));
    });

    test('toJson omits null optional fields', () {
      final item = WorkoutSyncItem(
        source: 'MANUAL',
        startedAt: DateTime.utc(2026, 3, 21, 6, 0, 0),
        distanceMeters: 5000.0,
        durationSeconds: 1500,
      );

      final json = item.toJson();

      expect(json.containsKey('sourceId'), isFalse);
      expect(json.containsKey('avgHr'), isFalse);
      expect(json.containsKey('maxHr'), isFalse);
      expect(json.containsKey('elevationGain'), isFalse);
    });

    test('toJson formats startedAt as UTC ISO 8601', () {
      final item = WorkoutSyncItem(
        source: 'APPLE_HEALTH',
        startedAt: DateTime.utc(2026, 3, 20, 7, 0, 0),
        distanceMeters: 1000.0,
        durationSeconds: 300,
      );

      final json = item.toJson();

      expect(json['startedAt'], equals('2026-03-20T07:00:00.000Z'));
    });
  });

  group('HealthSnapshotSyncItem', () {
    test('toJson includes all optional fields when set', () {
      final item = HealthSnapshotSyncItem(
        vo2maxEstimate: 52.3,
        restingHr: 55,
        recordedAt: DateTime.utc(2026, 3, 20),
      );

      final json = item.toJson();

      expect(json['vo2maxEstimate'], equals(52.3));
      expect(json['restingHr'], equals(55));
    });

    test('toJson omits null optional fields', () {
      final item = HealthSnapshotSyncItem(
        recordedAt: DateTime.utc(2026, 3, 20),
      );

      final json = item.toJson();

      expect(json.containsKey('vo2maxEstimate'), isFalse);
      expect(json.containsKey('restingHr'), isFalse);
    });
  });

  group('HealthSyncRequest', () {
    test('toJson serializes workouts and snapshots', () {
      final request = HealthSyncRequest(
        workouts: [
          WorkoutSyncItem(
            source: 'APPLE_HEALTH',
            startedAt: DateTime.utc(2026, 3, 20, 7, 0, 0),
            distanceMeters: 10000.0,
            durationSeconds: 3000,
          ),
        ],
        healthSnapshots: [
          HealthSnapshotSyncItem(
            vo2maxEstimate: 52.0,
            recordedAt: DateTime.utc(2026, 3, 20),
          ),
        ],
      );

      final json = request.toJson();

      expect(json['workouts'], isA<List>());
      expect((json['workouts'] as List).length, equals(1));
      expect(json['healthSnapshots'], isA<List>());
      expect((json['healthSnapshots'] as List).length, equals(1));
    });

    test('toJson handles empty lists', () {
      const request = HealthSyncRequest(workouts: [], healthSnapshots: []);

      final json = request.toJson();

      expect((json['workouts'] as List).isEmpty, isTrue);
      expect((json['healthSnapshots'] as List).isEmpty, isTrue);
    });
  });

  group('HealthSyncResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'workoutsSaved': 5,
        'workoutsSkipped': 2,
        'workoutsMatched': 1,
        'snapshotsSaved': 3,
        'vdotUpdated': true,
        'adjustmentApplied': 'MINOR',
      };

      final response = HealthSyncResponse.fromJson(json);

      expect(response.workoutsSaved, equals(5));
      expect(response.workoutsSkipped, equals(2));
      expect(response.workoutsMatched, equals(1));
      expect(response.snapshotsSaved, equals(3));
      expect(response.vdotUpdated, isTrue);
      expect(response.adjustmentApplied, equals('MINOR'));
    });
  });
}
