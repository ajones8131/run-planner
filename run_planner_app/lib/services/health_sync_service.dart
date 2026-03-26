import '../config/api_config.dart';
import '../models/health_sync.dart';
import 'api_client.dart';

class HealthSyncService {
  final ApiClient _apiClient;
  HealthSyncService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<HealthSyncResponse> sync(HealthSyncRequest request) async {
    final json = await _apiClient.post(ApiConfig.healthSync, body: request.toJson());
    return HealthSyncResponse.fromJson(json);
  }
}
