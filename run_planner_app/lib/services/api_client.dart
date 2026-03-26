import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  const ApiException({required this.statusCode, required this.message});

  @override
  String toString() => 'ApiException($statusCode): $message';
}

class ApiClient {
  final http.Client _httpClient;
  final String? Function() _getToken;
  final Future<void> Function()? onUnauthorized;

  ApiClient({
    http.Client? httpClient,
    required String? Function() getToken,
    this.onUnauthorized,
  })  : _httpClient = httpClient ?? http.Client(),
        _getToken = getToken;

  Map<String, String> _headers() {
    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    final token = _getToken();
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  Future<Map<String, dynamic>> get(String path, {Map<String, String>? queryParams}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path').replace(queryParameters: queryParams);
    final response = await _httpClient.get(uri, headers: _headers());
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> post(String path, {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final response = await _httpClient.post(uri, headers: _headers(), body: body != null ? jsonEncode(body) : null);
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> patch(String path, {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final response = await _httpClient.patch(uri, headers: _headers(), body: body != null ? jsonEncode(body) : null);
    return _handleResponse(response);
  }

  Future<void> delete(String path) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final response = await _httpClient.delete(uri, headers: _headers());
    if (response.statusCode == 401 && onUnauthorized != null) {
      await onUnauthorized!();
    }
    if (response.statusCode >= 300) {
      throw ApiException(statusCode: response.statusCode, message: response.body);
    }
  }

  Future<List<dynamic>> getList(String path, {Map<String, String>? queryParams}) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path').replace(queryParameters: queryParams);
    final response = await _httpClient.get(uri, headers: _headers());
    if (response.statusCode == 401 && onUnauthorized != null) {
      await onUnauthorized!();
    }
    if (response.statusCode >= 300) {
      throw ApiException(statusCode: response.statusCode, message: response.body);
    }
    return jsonDecode(response.body) as List<dynamic>;
  }

  Map<String, dynamic> _handleResponse(http.Response response) {
    if (response.statusCode == 401 && onUnauthorized != null) {
      onUnauthorized!();
    }
    if (response.statusCode >= 300) {
      throw ApiException(statusCode: response.statusCode, message: response.body);
    }
    if (response.body.isEmpty) return {};
    return jsonDecode(response.body) as Map<String, dynamic>;
  }
}
