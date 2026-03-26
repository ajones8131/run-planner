import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/user.dart';

void main() {
  group('Units', () {
    test('fromJson parses METRIC', () {
      expect(Units.fromJson('METRIC'), equals(Units.metric));
    });

    test('fromJson parses IMPERIAL', () {
      expect(Units.fromJson('IMPERIAL'), equals(Units.imperial));
    });

    test('fromJson throws on unknown value', () {
      expect(() => Units.fromJson('UNKNOWN'), throwsArgumentError);
    });

    test('toJson returns uppercase name', () {
      expect(Units.metric.toJson(), equals('METRIC'));
      expect(Units.imperial.toJson(), equals('IMPERIAL'));
    });
  });

  group('UserResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'id': 'user-123',
        'email': 'aaron@example.com',
        'name': 'Aaron',
        'dateOfBirth': '1990-05-15',
        'maxHr': 185,
        'preferredUnits': 'METRIC',
      };

      final response = UserResponse.fromJson(json);

      expect(response.id, equals('user-123'));
      expect(response.email, equals('aaron@example.com'));
      expect(response.name, equals('Aaron'));
      expect(response.dateOfBirth, equals(DateTime(1990, 5, 15)));
      expect(response.maxHr, equals(185));
      expect(response.preferredUnits, equals(Units.metric));
    });

    test('fromJson handles null optional fields', () {
      final json = {
        'id': 'user-456',
        'email': 'user@example.com',
        'name': null,
        'dateOfBirth': null,
        'maxHr': null,
        'preferredUnits': 'IMPERIAL',
      };

      final response = UserResponse.fromJson(json);

      expect(response.name, isNull);
      expect(response.dateOfBirth, isNull);
      expect(response.maxHr, isNull);
      expect(response.preferredUnits, equals(Units.imperial));
    });
  });

  group('UpdateProfileRequest', () {
    test('toJson includes all fields when set', () {
      final request = UpdateProfileRequest(
        name: 'Aaron',
        dateOfBirth: DateTime(1990, 5, 15),
        maxHr: 185,
        preferredUnits: Units.metric,
      );

      final json = request.toJson();

      expect(json['name'], equals('Aaron'));
      expect(json['dateOfBirth'], equals('1990-05-15'));
      expect(json['maxHr'], equals(185));
      expect(json['preferredUnits'], equals('METRIC'));
    });

    test('toJson omits null fields', () {
      const request = UpdateProfileRequest();

      final json = request.toJson();

      expect(json.isEmpty, isTrue);
    });

    test('toJson formats dateOfBirth with zero-padded month and day', () {
      final request = UpdateProfileRequest(dateOfBirth: DateTime(2000, 1, 3));

      final json = request.toJson();

      expect(json['dateOfBirth'], equals('2000-01-03'));
    });
  });
}
