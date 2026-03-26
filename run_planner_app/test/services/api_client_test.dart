import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:run_planner_app/services/api_client.dart';
import 'package:run_planner_app/config/api_config.dart';

void main() {
  group('ApiClient', () {
    test('get includes auth header when token is set', () async {
      String? capturedAuthHeader;
      final mockClient = MockClient((request) async {
        capturedAuthHeader = request.headers['Authorization'];
        return http.Response('{"ok": true}', 200);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => 'test-jwt-token');
      await apiClient.get(ApiConfig.userProfile);
      expect(capturedAuthHeader, 'Bearer test-jwt-token');
    });

    test('get omits auth header when token is null', () async {
      String? capturedAuthHeader;
      final mockClient = MockClient((request) async {
        capturedAuthHeader = request.headers['Authorization'];
        return http.Response('{"ok": true}', 200);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => null);
      await apiClient.get(ApiConfig.userProfile);
      expect(capturedAuthHeader, isNull);
    });

    test('post sends JSON body', () async {
      String? capturedBody;
      String? capturedContentType;
      final mockClient = MockClient((request) async {
        capturedBody = request.body;
        capturedContentType = request.headers['Content-Type'];
        return http.Response('{"id": "123"}', 200);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => 'token');
      await apiClient.post(ApiConfig.register, body: {'email': 'test@example.com', 'password': 'pass123'});
      expect(capturedContentType, 'application/json');
      final decoded = jsonDecode(capturedBody!);
      expect(decoded['email'], 'test@example.com');
    });

    test('throws ApiException on non-2xx response', () async {
      final mockClient = MockClient((request) async {
        return http.Response('{"message": "Not found"}', 404);
      });
      final apiClient = ApiClient(httpClient: mockClient, getToken: () => 'token');
      expect(
        () => apiClient.get('/api/v1/nonexistent'),
        throwsA(isA<ApiException>().having((e) => e.statusCode, 'statusCode', 404)),
      );
    });
  });
}
