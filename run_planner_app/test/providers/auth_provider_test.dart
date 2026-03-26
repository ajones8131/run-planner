import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/providers/auth_provider.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/services/auth_service.dart';

/// A fake secure storage for testing (in-memory map).
class FakeTokenStorage implements TokenStorage {
  final Map<String, String> _store = {};

  @override
  Future<String?> read(String key) async => _store[key];

  @override
  Future<void> write(String key, String value) async => _store[key] = value;

  @override
  Future<void> delete(String key) async => _store.remove(key);
}

void main() {
  group('AuthProvider', () {
    late AuthProvider provider;
    late FakeTokenStorage storage;

    setUp(() {
      final mockClient = MockClient((request) async {
        if (request.url.path.endsWith('/login')) {
          return http.Response(jsonEncode({'accessToken': 'access-token', 'refreshToken': 'refresh-token'}), 200);
        }
        if (request.url.path.endsWith('/register')) {
          return http.Response(jsonEncode({'accessToken': 'new-access', 'refreshToken': 'new-refresh'}), 200);
        }
        if (request.url.path.endsWith('/logout')) {
          return http.Response('', 200);
        }
        return http.Response('Not found', 404);
      });

      storage = FakeTokenStorage();
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => null);
      final authService = AuthService(apiClient: apiClient);
      provider = AuthProvider(authService: authService, tokenStorage: storage);
    });

    test('login stores tokens and sets authenticated', () async {
      await provider.login('test@example.com', 'password');
      expect(provider.isAuthenticated, true);
      expect(provider.accessToken, 'access-token');
      expect(await storage.read('access_token'), 'access-token');
      expect(await storage.read('refresh_token'), 'refresh-token');
    });

    test('register stores tokens and sets authenticated', () async {
      await provider.register('test@example.com', 'password', 'Test');
      expect(provider.isAuthenticated, true);
      expect(provider.accessToken, 'new-access');
    });

    test('logout clears tokens', () async {
      await provider.login('test@example.com', 'password');
      await provider.logout();
      expect(provider.isAuthenticated, false);
      expect(provider.accessToken, isNull);
      expect(await storage.read('access_token'), isNull);
    });

    test('tryRestoreSession loads tokens from storage', () async {
      await storage.write('access_token', 'stored-access');
      await storage.write('refresh_token', 'stored-refresh');
      await provider.tryRestoreSession();
      expect(provider.isAuthenticated, true);
      expect(provider.accessToken, 'stored-access');
    });

    test('tryRestoreSession with no stored tokens stays unauthenticated', () async {
      await provider.tryRestoreSession();
      expect(provider.isAuthenticated, false);
    });
  });
}
