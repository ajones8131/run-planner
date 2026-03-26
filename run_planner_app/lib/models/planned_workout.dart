enum WorkoutType {
  easy('Easy Run', 'E'),
  long_('Long Run', 'L'),
  marathon('Marathon Pace', 'M'),
  threshold('Threshold Run', 'T'),
  interval('Intervals', 'I'),
  repetition('Repetitions', 'R'),
  rest('Rest Day', '-');

  final String displayName;
  final String abbreviation;

  const WorkoutType(this.displayName, this.abbreviation);

  static WorkoutType fromJson(String value) {
    return switch (value) {
      'EASY' => WorkoutType.easy,
      'LONG' => WorkoutType.long_,
      'MARATHON' => WorkoutType.marathon,
      'THRESHOLD' => WorkoutType.threshold,
      'INTERVAL' => WorkoutType.interval,
      'REPETITION' => WorkoutType.repetition,
      'REST' => WorkoutType.rest,
      _ => throw ArgumentError('Unknown WorkoutType: $value'),
    };
  }

  String toJson() {
    return switch (this) {
      WorkoutType.long_ => 'LONG',
      _ => name.toUpperCase(),
    };
  }
}

enum TrainingPhase {
  base,
  quality,
  peak,
  taper;

  static TrainingPhase fromJson(String value) {
    return switch (value) {
      'BASE' => TrainingPhase.base,
      'QUALITY' => TrainingPhase.quality,
      'PEAK' => TrainingPhase.peak,
      'TAPER' => TrainingPhase.taper,
      _ => throw ArgumentError('Unknown TrainingPhase: $value'),
    };
  }

  String get displayName =>
      '${name[0].toUpperCase()}${name.substring(1)}';
}

class PlannedWorkoutResponse {
  final String id;
  final int weekNumber;
  final int dayOfWeek;
  final DateTime scheduledDate;
  final WorkoutType workoutType;
  final double targetDistanceMeters;
  final double? targetPaceMinPerKm;
  final double? targetPaceMaxPerKm;
  final int? targetHrZone;
  final String? notes;
  final int planRevision;

  const PlannedWorkoutResponse({
    required this.id,
    required this.weekNumber,
    required this.dayOfWeek,
    required this.scheduledDate,
    required this.workoutType,
    required this.targetDistanceMeters,
    this.targetPaceMinPerKm,
    this.targetPaceMaxPerKm,
    this.targetHrZone,
    this.notes,
    required this.planRevision,
  });

  factory PlannedWorkoutResponse.fromJson(Map<String, dynamic> json) {
    return PlannedWorkoutResponse(
      id: json['id'] as String,
      weekNumber: json['weekNumber'] as int,
      dayOfWeek: json['dayOfWeek'] as int,
      scheduledDate: DateTime.parse(json['scheduledDate'] as String),
      workoutType: WorkoutType.fromJson(json['workoutType'] as String),
      targetDistanceMeters:
          (json['targetDistanceMeters'] as num).toDouble(),
      targetPaceMinPerKm:
          (json['targetPaceMinPerKm'] as num?)?.toDouble(),
      targetPaceMaxPerKm:
          (json['targetPaceMaxPerKm'] as num?)?.toDouble(),
      targetHrZone: json['targetHrZone'] as int?,
      notes: json['notes'] as String?,
      planRevision: json['planRevision'] as int,
    );
  }
}
