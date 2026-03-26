import 'package:flutter_test/flutter_test.dart';
import 'package:run_planner_app/models/auth.dart';

void main() {
  group('AuthResponse', () {
    test('fromJson parses all fields', () {
      final json = {
        'accessToken': 'access-token-value',
        'refreshToken': 'refresh-token-value',
      };

      final response = AuthResponse.fromJson(json);

      expect(response.accessToken, equals('access-token-value'));
      expect(response.refreshToken, equals('refresh-token-value'));
    });
  });

  group('RegisterRequest', () {
    test('toJson includes all fields when name is provided', () {
      const request = RegisterRequest(
        email: 'user@example.com',
        password: 'secret123',
        name: 'Aaron',
      );

      final json = request.toJson();

      expect(json['email'], equals('user@example.com'));
      expect(json['password'], equals('secret123'));
      expect(json['name'], equals('Aaron'));
    });

    test('toJson omits name when null', () {
      const request = RegisterRequest(
        email: 'user@example.com',
        password: 'secret123',
      );

      final json = request.toJson();

      expect(json['email'], equals('user@example.com'));
      expect(json['password'], equals('secret123'));
      expect(json.containsKey('name'), isFalse);
    });
  });

  group('LoginRequest', () {
    test('toJson includes email and password', () {
      const request = LoginRequest(
        email: 'user@example.com',
        password: 'secret123',
      );

      final json = request.toJson();

      expect(json['email'], equals('user@example.com'));
      expect(json['password'], equals('secret123'));
    });
  });

  group('RefreshRequest', () {
    test('toJson includes refreshToken', () {
      const request = RefreshRequest(refreshToken: 'my-refresh-token');

      final json = request.toJson();

      expect(json['refreshToken'], equals('my-refresh-token'));
    });
  });
}
