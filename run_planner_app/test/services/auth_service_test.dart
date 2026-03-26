import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/models/auth.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/auth_service.dart';

void main() {
  group('AuthService', () {
    test('register sends credentials and returns tokens', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/register');
        final body = jsonDecode(request.body);
        expect(body['email'], 'test@example.com');
        return http.Response(jsonEncode({'accessToken': 'access-123', 'refreshToken': 'refresh-123'}), 200);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => null);
      final authService = AuthService(apiClient: apiClient);
      final response = await authService.register(const RegisterRequest(email: 'test@example.com', password: 'password123', name: 'Test'));
      expect(response.accessToken, 'access-123');
      expect(response.refreshToken, 'refresh-123');
    });

    test('login sends credentials and returns tokens', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/login');
        return http.Response(jsonEncode({'accessToken': 'access-456', 'refreshToken': 'refresh-456'}), 200);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => null);
      final authService = AuthService(apiClient: apiClient);
      final response = await authService.login(const LoginRequest(email: 'test@example.com', password: 'pass'));
      expect(response.accessToken, 'access-456');
    });

    test('refresh exchanges refresh token for new pair', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, '/api/v1/auth/refresh');
        return http.Response(jsonEncode({'accessToken': 'new-access', 'refreshToken': 'new-refresh'}), 200);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => null);
      final authService = AuthService(apiClient: apiClient);
      final response = await authService.refresh(const RefreshRequest(refreshToken: 'old-refresh'));
      expect(response.accessToken, 'new-access');
    });
  });
}
