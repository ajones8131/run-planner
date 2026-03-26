class WorkoutResponse {
  final String id;
  final DateTime startedAt;
  final double distanceMeters;
  final int durationSeconds;
  final int? avgHr;
  final int? maxHr;
  final double? elevationGain;
  final String source;
  final String sourceId;

  const WorkoutResponse({
    required this.id,
    required this.startedAt,
    required this.distanceMeters,
    required this.durationSeconds,
    this.avgHr,
    this.maxHr,
    this.elevationGain,
    required this.source,
    required this.sourceId,
  });

  double get paceMinPerKm {
    if (distanceMeters <= 0) return 0;
    return (durationSeconds / 60) / (distanceMeters / 1000);
  }

  factory WorkoutResponse.fromJson(Map<String, dynamic> json) {
    return WorkoutResponse(
      id: json['id'] as String,
      startedAt: DateTime.parse(json['startedAt'] as String),
      distanceMeters: (json['distanceMeters'] as num).toDouble(),
      durationSeconds: json['durationSeconds'] as int,
      avgHr: json['avgHr'] as int?,
      maxHr: json['maxHr'] as int?,
      elevationGain: (json['elevationGain'] as num?)?.toDouble(),
      source: json['source'] as String,
      sourceId: json['sourceId'] as String,
    );
  }
}
