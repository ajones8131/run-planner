import '../config/api_config.dart';
import '../models/planned_workout.dart';
import '../models/training_plan.dart';
import 'api_client.dart';

class PlanService {
  final ApiClient _apiClient;
  PlanService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<TrainingPlanResponse> create(CreatePlanRequest request) async {
    final json = await _apiClient.post(ApiConfig.plans, body: request.toJson());
    return TrainingPlanResponse.fromJson(json);
  }

  Future<TrainingPlanResponse> getActive() async {
    final json = await _apiClient.get(ApiConfig.activePlan);
    return TrainingPlanResponse.fromJson(json);
  }

  Future<List<PlannedWorkoutResponse>> getWorkouts(
    String planId, {
    DateTime? from,
    DateTime? to,
  }) async {
    final params = <String, String>{};
    if (from != null) {
      params['from'] =
          '${from.year}-${from.month.toString().padLeft(2, '0')}-${from.day.toString().padLeft(2, '0')}';
    }
    if (to != null) {
      params['to'] =
          '${to.year}-${to.month.toString().padLeft(2, '0')}-${to.day.toString().padLeft(2, '0')}';
    }
    final jsonList = await _apiClient.getList(
      ApiConfig.planWorkouts(planId),
      queryParams: params.isNotEmpty ? params : null,
    );
    return jsonList
        .map((j) => PlannedWorkoutResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<void> archive(String planId) async {
    await _apiClient.delete(ApiConfig.plan(planId));
  }
}
