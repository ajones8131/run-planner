import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:health/health.dart';
import '../models/health_sync.dart';

class HealthKitService {
  Health? _health;

  bool get _isSupported => !kIsWeb && Platform.isIOS;

  Health get _instance => _health ??= Health();

  Future<bool> requestPermissions() async {
    if (!_isSupported) return false;
    final types = [
      HealthDataType.WORKOUT,
      HealthDataType.HEART_RATE,
      HealthDataType.RESTING_HEART_RATE,
    ];
    final permissions = types.map((_) => HealthDataAccess.READ).toList();
    return await _instance.requestAuthorization(types, permissions: permissions);
  }

  Future<List<WorkoutSyncItem>> getWorkouts(DateTime since) async {
    if (!_isSupported) return [];
    final healthData = await _instance.getHealthDataFromTypes(
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

  Future<HealthSnapshotSyncItem?> getLatestHealthSnapshot(DateTime since) async {
    if (!_isSupported) return null;
    final data = await _instance.getHealthDataFromTypes(
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
