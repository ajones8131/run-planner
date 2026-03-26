import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/models/user.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/user_service.dart';

void main() {
  group('UserService', () {
    test('getProfile returns user', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/users/me');
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
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => 'token');
      final service = UserService(apiClient: apiClient);
      final user = await service.getProfile();
      expect(user.name, 'Aaron');
      expect(user.maxHr, 185);
    });

    test('updateProfile sends patch and returns updated user', () async {
      final mockClient = MockClient((request) async {
        expect(request.method, 'PATCH');
        return http.Response(
          jsonEncode({
            'id': 'user-uuid',
            'email': 'test@example.com',
            'name': 'Updated',
            'dateOfBirth': '1990-05-15',
            'maxHr': 190,
            'preferredUnits': 'METRIC',
          }),
          200,
        );
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => 'token');
      final service = UserService(apiClient: apiClient);
      final user = await service.updateProfile(const UpdateProfileRequest(name: 'Updated', maxHr: 190));
      expect(user.name, 'Updated');
      expect(user.maxHr, 190);
    });
  });
}
