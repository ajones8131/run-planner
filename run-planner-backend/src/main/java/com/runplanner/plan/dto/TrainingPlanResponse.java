package com.runplanner.plan.dto;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.TrainingPlan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrainingPlanResponse(
        UUID id,
        UUID goalRaceId,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        int revision,
        Instant createdAt,
        List<PlannedWorkoutResponse> workouts
) {
    public static TrainingPlanResponse from(TrainingPlan plan) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getGoalRace().getId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus().name(),
                plan.getRevision(),
                plan.getCreatedAt(),
                null
        );
    }

    public static TrainingPlanResponse from(TrainingPlan plan, List<PlannedWorkout> workouts) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getGoalRace().getId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus().name(),
                plan.getRevision(),
                plan.getCreatedAt(),
                workouts.stream().map(PlannedWorkoutResponse::from).toList()
        );
    }
}
