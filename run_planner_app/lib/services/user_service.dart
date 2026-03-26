import '../config/api_config.dart';
import '../models/user.dart';
import 'api_client.dart';

class UserService {
  final ApiClient _apiClient;
  UserService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<UserResponse> getProfile() async {
    final json = await _apiClient.get(ApiConfig.userProfile);
    return UserResponse.fromJson(json);
  }

  Future<UserResponse> updateProfile(UpdateProfileRequest request) async {
    final json = await _apiClient.patch(ApiConfig.userProfile, body: request.toJson());
    return UserResponse.fromJson(json);
  }
}
