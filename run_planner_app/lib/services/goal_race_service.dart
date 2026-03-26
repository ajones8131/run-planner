import '../config/api_config.dart';
import '../models/goal_race.dart';
import 'api_client.dart';

class GoalRaceService {
  final ApiClient _apiClient;
  GoalRaceService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<GoalRaceResponse> create(CreateGoalRaceRequest request) async {
    final json = await _apiClient.post(ApiConfig.goalRaces, body: request.toJson());
    return GoalRaceResponse.fromJson(json);
  }

  Future<List<GoalRaceResponse>> list() async {
    final jsonList = await _apiClient.getList(ApiConfig.goalRaces);
    return jsonList.map((j) => GoalRaceResponse.fromJson(j as Map<String, dynamic>)).toList();
  }

  Future<GoalRaceResponse> update(String id, UpdateGoalRaceRequest request) async {
    final json = await _apiClient.patch(ApiConfig.goalRace(id), body: request.toJson());
    return GoalRaceResponse.fromJson(json);
  }
}
