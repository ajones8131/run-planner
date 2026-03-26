import '../config/api_config.dart';
import '../models/auth.dart';
import 'api_client.dart';

class AuthService {
  final ApiClient _apiClient;

  AuthService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<AuthResponse> register(RegisterRequest request) async {
    final json = await _apiClient.post(ApiConfig.register, body: request.toJson());
    return AuthResponse.fromJson(json);
  }

  Future<AuthResponse> login(LoginRequest request) async {
    final json = await _apiClient.post(ApiConfig.login, body: request.toJson());
    return AuthResponse.fromJson(json);
  }

  Future<AuthResponse> refresh(RefreshRequest request) async {
    final json = await _apiClient.post(ApiConfig.refresh, body: request.toJson());
    return AuthResponse.fromJson(json);
  }

  Future<void> logout() async {
    await _apiClient.post(ApiConfig.logout);
  }
}
