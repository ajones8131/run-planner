package com.runplanner.workout.dto;

import com.runplanner.workout.Workout;

import java.time.Instant;
import java.util.UUID;

public record WorkoutResponse(
        UUID id,
        Instant startedAt,
        double distanceMeters,
        int durationSeconds,
        Integer avgHr,
        Integer maxHr,
        Double elevationGain,
        String source,
        String sourceId
) {
    public static WorkoutResponse from(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getStartedAt(),
                workout.getDistanceMeters(),
                workout.getDurationSeconds(),
                workout.getAvgHr(),
                workout.getMaxHr(),
                workout.getElevationGain(),
                workout.getSource(),
                workout.getSourceId()
        );
    }
}
