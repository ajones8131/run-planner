import 'package:flutter/foundation.dart';
import '../models/health_sync.dart';
import '../services/health_kit_service.dart';
import '../services/health_sync_service.dart';

class SyncProvider extends ChangeNotifier {
  final HealthKitService _healthKitService;
  final HealthSyncService _healthSyncService;
  DateTime? _lastSyncedAt;
  bool _syncing = false;
  String? _error;
  HealthSyncResponse? _lastSyncResult;

  SyncProvider({
    required HealthKitService healthKitService,
    required HealthSyncService healthSyncService,
  })  : _healthKitService = healthKitService,
        _healthSyncService = healthSyncService;

  DateTime? get lastSyncedAt => _lastSyncedAt;
  bool get syncing => _syncing;
  String? get error => _error;
  HealthSyncResponse? get lastSyncResult => _lastSyncResult;

  Future<bool> requestPermissions() async {
    return await _healthKitService.requestPermissions();
  }

  Future<void> sync({DateTime? since}) async {
    if (_syncing) return;
    _syncing = true;
    _error = null;
    notifyListeners();
    try {
      final syncSince = since ?? _lastSyncedAt ?? DateTime.now().subtract(const Duration(days: 30));
      final workouts = await _healthKitService.getWorkouts(syncSince);
      final snapshot = await _healthKitService.getLatestHealthSnapshot(syncSince);
      final request = HealthSyncRequest(
        workouts: workouts,
        healthSnapshots: snapshot != null ? [snapshot] : [],
      );
      _lastSyncResult = await _healthSyncService.sync(request);
      _lastSyncedAt = DateTime.now();
    } catch (e) {
      _error = 'Sync failed';
    }
    _syncing = false;
    notifyListeners();
  }
}
