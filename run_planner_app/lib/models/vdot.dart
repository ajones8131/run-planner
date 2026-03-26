enum TrainingZone {
  easy('Easy', 'E'),
  marathon('Marathon', 'M'),
  threshold('Threshold', 'T'),
  interval('Interval', 'I'),
  repetition('Repetition', 'R');

  final String displayName;
  final String abbreviation;

  const TrainingZone(this.displayName, this.abbreviation);

  static TrainingZone fromJson(String value) {
    return switch (value) {
      'E' => TrainingZone.easy,
      'M' => TrainingZone.marathon,
      'T' => TrainingZone.threshold,
      'I' => TrainingZone.interval,
      'R' => TrainingZone.repetition,
      _ => throw ArgumentError('Unknown TrainingZone: $value'),
    };
  }
}

class VdotHistoryResponse {
  final String id;
  final String? triggeringWorkoutId;
  final String? triggeringSnapshotId;
  final double previousVdot;
  final double newVdot;
  final DateTime calculatedAt;
  final bool flagged;
  final bool accepted;

  const VdotHistoryResponse({
    required this.id,
    this.triggeringWorkoutId,
    this.triggeringSnapshotId,
    required this.previousVdot,
    required this.newVdot,
    required this.calculatedAt,
    required this.flagged,
    required this.accepted,
  });

  factory VdotHistoryResponse.fromJson(Map<String, dynamic> json) {
    return VdotHistoryResponse(
      id: json['id'] as String,
      triggeringWorkoutId: json['triggeringWorkoutId'] as String?,
      triggeringSnapshotId: json['triggeringSnapshotId'] as String?,
      previousVdot: (json['previousVdot'] as num).toDouble(),
      newVdot: (json['newVdot'] as num).toDouble(),
      calculatedAt: DateTime.parse(json['calculatedAt'] as String),
      flagged: json['flagged'] as bool,
      accepted: json['accepted'] as bool,
    );
  }
}
