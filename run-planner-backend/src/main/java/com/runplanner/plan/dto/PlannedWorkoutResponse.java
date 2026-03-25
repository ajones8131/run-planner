package com.runplanner.plan.dto;

import com.runplanner.plan.PlannedWorkout;

import java.time.LocalDate;
import java.util.UUID;

public record PlannedWorkoutResponse(
        UUID id,
        int weekNumber,
        int dayOfWeek,
        LocalDate scheduledDate,
        String workoutType,
        double targetDistanceMeters,
        Double targetPaceMinPerKm,
        Double targetPaceMaxPerKm,
        Integer targetHrZone,
        String notes,
        int planRevision
) {
    public static PlannedWorkoutResponse from(PlannedWorkout pw) {
        return new PlannedWorkoutResponse(
                pw.getId(),
                pw.getWeekNumber(),
                pw.getDayOfWeek(),
                pw.getScheduledDate(),
                pw.getWorkoutType().name(),
                pw.getTargetDistanceMeters(),
                pw.getTargetPaceMinPerKm(),
                pw.getTargetPaceMaxPerKm(),
                pw.getTargetHrZone(),
                pw.getNotes(),
                pw.getPlanRevision()
        );
    }
}
