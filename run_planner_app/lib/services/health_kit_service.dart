import 'package:health/health.dart';
import '../models/health_sync.dart';

class HealthKitService {
  final Health _health = Health();

  Future<bool> requestPermissions() async {
    // TODO: Add HealthDataType.VO2MAX when available in the health package (not present in v11.x).
    final types = [
      HealthDataType.WORKOUT,
      HealthDataType.HEART_RATE,
      HealthDataType.RESTING_HEART_RATE,
    ];
    final permissions = types.map((_) => HealthDataAccess.READ).toList();
    return await _health.requestAuthorization(types, permissions: permissions);
  }

  Future<List<WorkoutSyncItem>> getWorkouts(DateTime since) async {
    final healthData = await _health.getHealthDataFromTypes(
      types: [HealthDataType.WORKOUT],
      startTime: since,
      endTime: DateTime.now(),
    );
    return healthData
        .where((d) => d.type == HealthDataType.WORKOUT)
        .map((d) {
          final value = d.value;
          if (value is WorkoutHealthValue) {
            return WorkoutSyncItem(
              source: 'APPLE_HEALTH',
              sourceId: d.uuid,
              startedAt: d.dateFrom,
              distanceMeters: value.totalDistance?.toDouble() ?? 0.0,
              durationSeconds: d.dateTo.difference(d.dateFrom).inSeconds,
            );
          }
          return null;
        })
        .whereType<WorkoutSyncItem>()
        .toList();
  }

  // TODO: Replace RESTING_HEART_RATE with VO2MAX once health package v12+ supports it.
  // Until then, we sync resting heart rate as a proxy health snapshot.
  Future<HealthSnapshotSyncItem?> getLatestHealthSnapshot(DateTime since) async {
    final data = await _health.getHealthDataFromTypes(
      types: [HealthDataType.RESTING_HEART_RATE],
      startTime: since,
      endTime: DateTime.now(),
    );
    if (data.isEmpty) return null;
    final latest = data.last;
    final value = latest.value;
    if (value is NumericHealthValue) {
      return HealthSnapshotSyncItem(
        restingHr: value.numericValue.toInt(),
        recordedAt: latest.dateFrom,
      );
    }
    return null;
  }
}
