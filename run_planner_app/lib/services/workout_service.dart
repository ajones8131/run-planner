import '../config/api_config.dart';
import '../models/workout.dart';
import 'api_client.dart';

class WorkoutService {
  final ApiClient _apiClient;
  WorkoutService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<List<WorkoutResponse>> list({DateTime? since}) async {
    final params = <String, String>{};
    if (since != null) params['since'] = since.toUtc().toIso8601String();
    final jsonList = await _apiClient.getList(
      ApiConfig.workouts,
      queryParams: params.isNotEmpty ? params : null,
    );
    return jsonList
        .map((j) => WorkoutResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<WorkoutResponse> get(String id) async {
    final json = await _apiClient.get(ApiConfig.workout(id));
    return WorkoutResponse.fromJson(json);
  }
}
