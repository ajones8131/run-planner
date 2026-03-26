import '../config/api_config.dart';
import '../models/vdot.dart';
import 'api_client.dart';

class VdotService {
  final ApiClient _apiClient;
  VdotService({required ApiClient apiClient}) : _apiClient = apiClient;

  Future<List<VdotHistoryResponse>> getHistory() async {
    final jsonList = await _apiClient.getList(ApiConfig.vdotHistory);
    return jsonList
        .map((j) => VdotHistoryResponse.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<void> accept(String id) async {
    await _apiClient.post(ApiConfig.vdotAccept(id));
  }

  Future<void> dismiss(String id) async {
    await _apiClient.post(ApiConfig.vdotDismiss(id));
  }
}
