import 'planned_workout.dart';

enum TrainingPlanStatus {
  active,
  completed,
  archived;

  static TrainingPlanStatus fromJson(String value) {
    return switch (value) {
      'ACTIVE' => TrainingPlanStatus.active,
      'COMPLETED' => TrainingPlanStatus.completed,
      'ARCHIVED' => TrainingPlanStatus.archived,
      _ => throw ArgumentError('Unknown TrainingPlanStatus: $value'),
    };
  }
}

class TrainingPlanResponse {
  final String id;
  final String goalRaceId;
  final DateTime startDate;
  final DateTime endDate;
  final TrainingPlanStatus status;
  final int revision;
  final DateTime createdAt;
  final List<PlannedWorkoutResponse> workouts;

  const TrainingPlanResponse({
    required this.id,
    required this.goalRaceId,
    required this.startDate,
    required this.endDate,
    required this.status,
    required this.revision,
    required this.createdAt,
    required this.workouts,
  });

  factory TrainingPlanResponse.fromJson(Map<String, dynamic> json) {
    return TrainingPlanResponse(
      id: json['id'] as String,
      goalRaceId: json['goalRaceId'] as String,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      status: TrainingPlanStatus.fromJson(json['status'] as String),
      revision: json['revision'] as int,
      createdAt: DateTime.parse(json['createdAt'] as String),
      workouts: (json['workouts'] as List<dynamic>?)
              ?.map(
                (w) => PlannedWorkoutResponse.fromJson(
                  w as Map<String, dynamic>,
                ),
              )
              .toList() ??
          [],
    );
  }
}

class CreatePlanRequest {
  final String goalRaceId;

  const CreatePlanRequest({required this.goalRaceId});

  Map<String, dynamic> toJson() => {'goalRaceId': goalRaceId};
}
