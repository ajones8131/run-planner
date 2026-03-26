import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/providers/user_provider.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/user_service.dart';
import 'package:run_planner_app/services/vdot_service.dart';

void main() {
  group('UserProvider', () {
    late UserProvider provider;

    setUp(() {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/v1/users/me') {
          return http.Response(
            jsonEncode({
              'id': 'user-uuid',
              'email': 'test@example.com',
              'name': 'Aaron',
              'dateOfBirth': '1990-05-15',
              'maxHr': 185,
              'preferredUnits': 'METRIC',
            }),
            200,
          );
        }
        if (request.url.path == '/api/v1/vdot/history') {
          return http.Response(
            jsonEncode([
              {
                'id': 'v1',
                'triggeringWorkoutId': null,
                'triggeringSnapshotId': 's1',
                'previousVdot': 45.0,
                'newVdot': 48.2,
                'calculatedAt': '2026-03-25T08:00:00Z',
                'flagged': false,
                'accepted': true,
              }
            ]),
            200,
          );
        }
        return http.Response('Not found', 404);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => 'token');
      provider = UserProvider(
        userService: UserService(apiClient: apiClient),
        vdotService: VdotService(apiClient: apiClient),
      );
    });

    test('loadProfile fetches user data', () async {
      await provider.loadProfile();
      expect(provider.user, isNotNull);
      expect(provider.user!.name, 'Aaron');
      expect(provider.user!.maxHr, 185);
    });

    test('loadVdotHistory fetches entries', () async {
      await provider.loadVdotHistory();
      expect(provider.vdotHistory, hasLength(1));
      expect(provider.vdotHistory.first.newVdot, 48.2);
    });

    test('currentVdot returns latest accepted entry', () async {
      await provider.loadVdotHistory();
      expect(provider.currentVdot, 48.2);
    });

    test('flaggedEntries returns entries needing review', () async {
      await provider.loadVdotHistory();
      expect(provider.flaggedEntries, isEmpty);
    });
  });
}
