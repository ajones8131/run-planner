import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/goal_race.dart';

void main() {
  group('GoalRaceStatus', () {
    test('fromJson parses ACTIVE', () {
      expect(GoalRaceStatus.fromJson('ACTIVE'), equals(GoalRaceStatus.active));
    });

    test('fromJson parses COMPLETED', () {
      expect(
        GoalRaceStatus.fromJson('COMPLETED'),
        equals(GoalRaceStatus.completed),
      );
    });

    test('fromJson parses ARCHIVED', () {
      expect(
        GoalRaceStatus.fromJson('ARCHIVED'),
        equals(GoalRaceStatus.archived),
      );
    });

    test('fromJson throws on unknown value', () {
      expect(() => GoalRaceStatus.fromJson('UNKNOWN'), throwsArgumentError);
    });

    test('toJson returns uppercase name', () {
      expect(GoalRaceStatus.active.toJson(), equals('ACTIVE'));
      expect(GoalRaceStatus.completed.toJson(), equals('COMPLETED'));
      expect(GoalRaceStatus.archived.toJson(), equals('ARCHIVED'));
    });
  });

  group('GoalRaceResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'race-123',
        'distanceMeters': 42195,
        'distanceLabel': 'Marathon',
        'raceDate': '2026-10-15',
        'goalFinishSeconds': 14400,
        'status': 'ACTIVE',
      };

      final response = GoalRaceResponse.fromJson(json);

      expect(response.id, equals('race-123'));
      expect(response.distanceMeters, equals(42195));
      expect(response.distanceLabel, equals('Marathon'));
      expect(response.raceDate, equals(DateTime(2026, 10, 15)));
      expect(response.goalFinishSeconds, equals(14400));
      expect(response.status, equals(GoalRaceStatus.active));
    });

    test('fromJson handles null goalFinishSeconds', () {
      final json = {
        'id': 'race-456',
        'distanceMeters': 21097,
        'distanceLabel': 'Half Marathon',
        'raceDate': '2026-06-01',
        'goalFinishSeconds': null,
        'status': 'ACTIVE',
      };

      final response = GoalRaceResponse.fromJson(json);

      expect(response.goalFinishSeconds, isNull);
    });
  });

  group('CreateGoalRaceRequest', () {
    test('toJson includes all fields when goalFinishSeconds is set', () {
      final request = CreateGoalRaceRequest(
        distanceMeters: 42195,
        distanceLabel: 'Marathon',
        raceDate: DateTime(2026, 10, 15),
        goalFinishSeconds: 14400,
      );

      final json = request.toJson();

      expect(json['distanceMeters'], equals(42195));
      expect(json['distanceLabel'], equals('Marathon'));
      expect(json['raceDate'], equals('2026-10-15'));
      expect(json['goalFinishSeconds'], equals(14400));
    });

    test('toJson omits goalFinishSeconds when null', () {
      final request = CreateGoalRaceRequest(
        distanceMeters: 42195,
        distanceLabel: 'Marathon',
        raceDate: DateTime(2026, 10, 15),
      );

      final json = request.toJson();

      expect(json.containsKey('goalFinishSeconds'), isFalse);
    });

    test('toJson formats raceDate with zero-padded month and day', () {
      final request = CreateGoalRaceRequest(
        distanceMeters: 5000,
        distanceLabel: '5K',
        raceDate: DateTime(2026, 1, 5),
      );

      final json = request.toJson();

      expect(json['raceDate'], equals('2026-01-05'));
    });
  });
}
