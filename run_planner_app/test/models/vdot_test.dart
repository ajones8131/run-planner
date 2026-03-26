import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/vdot.dart';

void main() {
  group('TrainingZone', () {
    test('fromJson parses all zones', () {
      expect(TrainingZone.fromJson('E'), equals(TrainingZone.easy));
      expect(TrainingZone.fromJson('M'), equals(TrainingZone.marathon));
      expect(TrainingZone.fromJson('T'), equals(TrainingZone.threshold));
      expect(TrainingZone.fromJson('I'), equals(TrainingZone.interval));
      expect(TrainingZone.fromJson('R'), equals(TrainingZone.repetition));
    });

    test('fromJson throws on unknown value', () {
      expect(() => TrainingZone.fromJson('X'), throwsArgumentError);
    });

    test('displayName and abbreviation are correct', () {
      expect(TrainingZone.easy.displayName, equals('Easy'));
      expect(TrainingZone.easy.abbreviation, equals('E'));
      expect(TrainingZone.marathon.displayName, equals('Marathon'));
      expect(TrainingZone.marathon.abbreviation, equals('M'));
      expect(TrainingZone.threshold.displayName, equals('Threshold'));
      expect(TrainingZone.threshold.abbreviation, equals('T'));
      expect(TrainingZone.interval.displayName, equals('Interval'));
      expect(TrainingZone.interval.abbreviation, equals('I'));
      expect(TrainingZone.repetition.displayName, equals('Repetition'));
      expect(TrainingZone.repetition.abbreviation, equals('R'));
    });
  });

  group('VdotHistoryResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'vdot-123',
        'triggeringWorkoutId': 'workout-abc',
        'triggeringSnapshotId': 'snapshot-xyz',
        'previousVdot': 45.0,
        'newVdot': 46.5,
        'calculatedAt': '2026-03-20T08:00:00.000Z',
        'flagged': false,
        'accepted': true,
      };

      final response = VdotHistoryResponse.fromJson(json);

      expect(response.id, equals('vdot-123'));
      expect(response.triggeringWorkoutId, equals('workout-abc'));
      expect(response.triggeringSnapshotId, equals('snapshot-xyz'));
      expect(response.previousVdot, equals(45.0));
      expect(response.newVdot, equals(46.5));
      expect(
        response.calculatedAt,
        equals(DateTime.utc(2026, 3, 20, 8, 0, 0)),
      );
      expect(response.flagged, isFalse);
      expect(response.accepted, isTrue);
    });

    test('fromJson handles null optional ids', () {
      final json = {
        'id': 'vdot-456',
        'triggeringWorkoutId': null,
        'triggeringSnapshotId': null,
        'previousVdot': 44.0,
        'newVdot': 44.0,
        'calculatedAt': '2026-03-01T12:00:00.000Z',
        'flagged': true,
        'accepted': false,
      };

      final response = VdotHistoryResponse.fromJson(json);

      expect(response.triggeringWorkoutId, isNull);
      expect(response.triggeringSnapshotId, isNull);
      expect(response.flagged, isTrue);
      expect(response.accepted, isFalse);
    });
  });
}
