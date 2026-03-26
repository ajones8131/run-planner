import 'package:flutter/foundation.dart';
import '../models/auth.dart';
import '../services/auth_service.dart';

/// Abstraction over secure storage so we can test without the real plugin.
abstract class TokenStorage {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
}

class AuthProvider extends ChangeNotifier {
  final AuthService _authService;
  final TokenStorage _tokenStorage;

  String? _accessToken;
  String? _refreshToken;

  AuthProvider({required AuthService authService, required TokenStorage tokenStorage})
      : _authService = authService,
        _tokenStorage = tokenStorage;

  bool get isAuthenticated => _accessToken != null;
  String? get accessToken => _accessToken;

  Future<void> login(String email, String password) async {
    final response = await _authService.login(LoginRequest(email: email, password: password));
    await _storeTokens(response);
  }

  Future<void> register(String email, String password, String? name) async {
    final response = await _authService.register(RegisterRequest(email: email, password: password, name: name));
    await _storeTokens(response);
  }

  Future<void> logout() async {
    try {
      await _authService.logout();
    } catch (_) {}
    await _clearTokens();
  }

  Future<void> tryRestoreSession() async {
    final accessToken = await _tokenStorage.read('access_token');
    final refreshToken = await _tokenStorage.read('refresh_token');
    if (accessToken != null && refreshToken != null) {
      _accessToken = accessToken;
      _refreshToken = refreshToken;
      notifyListeners();
    }
  }

  Future<bool> tryRefreshToken() async {
    if (_refreshToken == null) return false;
    try {
      final response = await _authService.refresh(RefreshRequest(refreshToken: _refreshToken!));
      await _storeTokens(response);
      return true;
    } catch (_) {
      await _clearTokens();
      return false;
    }
  }

  Future<void> _storeTokens(AuthResponse response) async {
    _accessToken = response.accessToken;
    _refreshToken = response.refreshToken;
    await _tokenStorage.write('access_token', response.accessToken);
    await _tokenStorage.write('refresh_token', response.refreshToken);
    notifyListeners();
  }

  Future<void> _clearTokens() async {
    _accessToken = null;
    _refreshToken = null;
    await _tokenStorage.delete('access_token');
    await _tokenStorage.delete('refresh_token');
    notifyListeners();
  }
}
