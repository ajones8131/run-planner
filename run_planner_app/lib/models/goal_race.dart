enum GoalRaceStatus {
  active,
  completed,
  archived;

  static GoalRaceStatus fromJson(String value) {
    return switch (value) {
      'ACTIVE' => GoalRaceStatus.active,
      'COMPLETED' => GoalRaceStatus.completed,
      'ARCHIVED' => GoalRaceStatus.archived,
      _ => throw ArgumentError('Unknown GoalRaceStatus: $value'),
    };
  }

  String toJson() => name.toUpperCase();
}

class GoalRaceResponse {
  final String id;
  final int distanceMeters;
  final String distanceLabel;
  final DateTime raceDate;
  final int? goalFinishSeconds;
  final GoalRaceStatus status;

  const GoalRaceResponse({
    required this.id,
    required this.distanceMeters,
    required this.distanceLabel,
    required this.raceDate,
    this.goalFinishSeconds,
    required this.status,
  });

  factory GoalRaceResponse.fromJson(Map<String, dynamic> json) {
    return GoalRaceResponse(
      id: json['id'] as String,
      distanceMeters: json['distanceMeters'] as int,
      distanceLabel: json['distanceLabel'] as String,
      raceDate: DateTime.parse(json['raceDate'] as String),
      goalFinishSeconds: json['goalFinishSeconds'] as int?,
      status: GoalRaceStatus.fromJson(json['status'] as String),
    );
  }
}

class CreateGoalRaceRequest {
  final int distanceMeters;
  final String distanceLabel;
  final DateTime raceDate;
  final int? goalFinishSeconds;

  const CreateGoalRaceRequest({
    required this.distanceMeters,
    required this.distanceLabel,
    required this.raceDate,
    this.goalFinishSeconds,
  });

  Map<String, dynamic> toJson() {
    return {
      'distanceMeters': distanceMeters,
      'distanceLabel': distanceLabel,
      'raceDate':
          '${raceDate.year}-${raceDate.month.toString().padLeft(2, '0')}-${raceDate.day.toString().padLeft(2, '0')}',
      if (goalFinishSeconds != null) 'goalFinishSeconds': goalFinishSeconds,
    };
  }
}

class UpdateGoalRaceRequest {
  final DateTime? raceDate;
  final int? goalFinishSeconds;
  final GoalRaceStatus? status;

  const UpdateGoalRaceRequest({
    this.raceDate,
    this.goalFinishSeconds,
    this.status,
  });

  Map<String, dynamic> toJson() {
    return {
      if (raceDate != null)
        'raceDate':
            '${raceDate!.year}-${raceDate!.month.toString().padLeft(2, '0')}-${raceDate!.day.toString().padLeft(2, '0')}',
      if (goalFinishSeconds != null) 'goalFinishSeconds': goalFinishSeconds,
      if (status != null) 'status': status!.toJson(),
    };
  }
}
