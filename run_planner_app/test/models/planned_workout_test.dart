import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/planned_workout.dart';

void main() {
  group('WorkoutType', () {
    test('fromJson parses all types', () {
      expect(WorkoutType.fromJson('EASY'), equals(WorkoutType.easy));
      expect(WorkoutType.fromJson('LONG'), equals(WorkoutType.long_));
      expect(WorkoutType.fromJson('MARATHON'), equals(WorkoutType.marathon));
      expect(WorkoutType.fromJson('THRESHOLD'), equals(WorkoutType.threshold));
      expect(WorkoutType.fromJson('INTERVAL'), equals(WorkoutType.interval));
      expect(WorkoutType.fromJson('REPETITION'), equals(WorkoutType.repetition));
      expect(WorkoutType.fromJson('REST'), equals(WorkoutType.rest));
    });

    test('fromJson throws on unknown value', () {
      expect(() => WorkoutType.fromJson('UNKNOWN'), throwsArgumentError);
    });

    test('toJson returns correct string for LONG type', () {
      expect(WorkoutType.long_.toJson(), equals('LONG'));
    });

    test('toJson returns uppercase name for standard types', () {
      expect(WorkoutType.easy.toJson(), equals('EASY'));
      expect(WorkoutType.marathon.toJson(), equals('MARATHON'));
      expect(WorkoutType.threshold.toJson(), equals('THRESHOLD'));
      expect(WorkoutType.interval.toJson(), equals('INTERVAL'));
      expect(WorkoutType.repetition.toJson(), equals('REPETITION'));
      expect(WorkoutType.rest.toJson(), equals('REST'));
    });

    test('displayName returns human-readable label', () {
      expect(WorkoutType.easy.displayName, equals('Easy Run'));
      expect(WorkoutType.long_.displayName, equals('Long Run'));
      expect(WorkoutType.rest.displayName, equals('Rest Day'));
    });

    test('abbreviation returns single-character code', () {
      expect(WorkoutType.easy.abbreviation, equals('E'));
      expect(WorkoutType.long_.abbreviation, equals('L'));
      expect(WorkoutType.marathon.abbreviation, equals('M'));
      expect(WorkoutType.threshold.abbreviation, equals('T'));
      expect(WorkoutType.interval.abbreviation, equals('I'));
      expect(WorkoutType.repetition.abbreviation, equals('R'));
      expect(WorkoutType.rest.abbreviation, equals('-'));
    });
  });

  group('TrainingPhase', () {
    test('fromJson parses all phases', () {
      expect(TrainingPhase.fromJson('BASE'), equals(TrainingPhase.base));
      expect(TrainingPhase.fromJson('QUALITY'), equals(TrainingPhase.quality));
      expect(TrainingPhase.fromJson('PEAK'), equals(TrainingPhase.peak));
      expect(TrainingPhase.fromJson('TAPER'), equals(TrainingPhase.taper));
    });

    test('fromJson throws on unknown value', () {
      expect(() => TrainingPhase.fromJson('UNKNOWN'), throwsArgumentError);
    });

    test('displayName returns capitalized name', () {
      expect(TrainingPhase.base.displayName, equals('Base'));
      expect(TrainingPhase.quality.displayName, equals('Quality'));
      expect(TrainingPhase.peak.displayName, equals('Peak'));
      expect(TrainingPhase.taper.displayName, equals('Taper'));
    });
  });

  group('PlannedWorkoutResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'workout-123',
        'weekNumber': 3,
        'dayOfWeek': 2,
        'scheduledDate': '2026-04-14',
        'workoutType': 'THRESHOLD',
        'targetDistanceMeters': 12000.0,
        'targetPaceMinPerKm': 4.5,
        'targetPaceMaxPerKm': 4.8,
        'targetHrZone': 4,
        'notes': 'Tempo run',
        'planRevision': 1,
      };

      final response = PlannedWorkoutResponse.fromJson(json);

      expect(response.id, equals('workout-123'));
      expect(response.weekNumber, equals(3));
      expect(response.dayOfWeek, equals(2));
      expect(response.scheduledDate, equals(DateTime(2026, 4, 14)));
      expect(response.workoutType, equals(WorkoutType.threshold));
      expect(response.targetDistanceMeters, equals(12000.0));
      expect(response.targetPaceMinPerKm, equals(4.5));
      expect(response.targetPaceMaxPerKm, equals(4.8));
      expect(response.targetHrZone, equals(4));
      expect(response.notes, equals('Tempo run'));
      expect(response.planRevision, equals(1));
    });

    test('fromJson parses REST day with null optional fields', () {
      final json = {
        'id': 'workout-rest',
        'weekNumber': 1,
        'dayOfWeek': 0,
        'scheduledDate': '2026-04-06',
        'workoutType': 'REST',
        'targetDistanceMeters': 0.0,
        'targetPaceMinPerKm': null,
        'targetPaceMaxPerKm': null,
        'targetHrZone': null,
        'notes': null,
        'planRevision': 1,
      };

      final response = PlannedWorkoutResponse.fromJson(json);

      expect(response.workoutType, equals(WorkoutType.rest));
      expect(response.targetDistanceMeters, equals(0.0));
      expect(response.targetPaceMinPerKm, isNull);
      expect(response.targetPaceMaxPerKm, isNull);
      expect(response.targetHrZone, isNull);
      expect(response.notes, isNull);
    });
  });
}
