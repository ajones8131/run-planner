class WorkoutSyncItem {
  final String source;
  final String? sourceId;
  final DateTime startedAt;
  final double distanceMeters;
  final int durationSeconds;
  final int? avgHr;
  final int? maxHr;
  final double? elevationGain;

  const WorkoutSyncItem({
    required this.source,
    this.sourceId,
    required this.startedAt,
    required this.distanceMeters,
    required this.durationSeconds,
    this.avgHr,
    this.maxHr,
    this.elevationGain,
  });

  Map<String, dynamic> toJson() {
    return {
      'source': source,
      if (sourceId != null) 'sourceId': sourceId,
      'startedAt': startedAt.toUtc().toIso8601String(),
      'distanceMeters': distanceMeters,
      'durationSeconds': durationSeconds,
      if (avgHr != null) 'avgHr': avgHr,
      if (maxHr != null) 'maxHr': maxHr,
      if (elevationGain != null) 'elevationGain': elevationGain,
    };
  }
}

class HealthSnapshotSyncItem {
  final double? vo2maxEstimate;
  final int? restingHr;
  final DateTime recordedAt;

  const HealthSnapshotSyncItem({
    this.vo2maxEstimate,
    this.restingHr,
    required this.recordedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      if (vo2maxEstimate != null) 'vo2maxEstimate': vo2maxEstimate,
      if (restingHr != null) 'restingHr': restingHr,
      'recordedAt': recordedAt.toUtc().toIso8601String(),
    };
  }
}

class HealthSyncRequest {
  final List<WorkoutSyncItem> workouts;
  final List<HealthSnapshotSyncItem> healthSnapshots;

  const HealthSyncRequest({
    required this.workouts,
    required this.healthSnapshots,
  });

  Map<String, dynamic> toJson() {
    return {
      'workouts': workouts.map((w) => w.toJson()).toList(),
      'healthSnapshots': healthSnapshots.map((s) => s.toJson()).toList(),
    };
  }
}

class HealthSyncResponse {
  final int workoutsSaved;
  final int workoutsSkipped;
  final int workoutsMatched;
  final int snapshotsSaved;
  final bool vdotUpdated;
  final String adjustmentApplied;

  const HealthSyncResponse({
    required this.workoutsSaved,
    required this.workoutsSkipped,
    required this.workoutsMatched,
    required this.snapshotsSaved,
    required this.vdotUpdated,
    required this.adjustmentApplied,
  });

  factory HealthSyncResponse.fromJson(Map<String, dynamic> json) {
    return HealthSyncResponse(
      workoutsSaved: json['workoutsSaved'] as int,
      workoutsSkipped: json['workoutsSkipped'] as int,
      workoutsMatched: json['workoutsMatched'] as int,
      snapshotsSaved: json['snapshotsSaved'] as int,
      vdotUpdated: json['vdotUpdated'] as bool,
      adjustmentApplied: json['adjustmentApplied'] as String,
    );
  }
}
