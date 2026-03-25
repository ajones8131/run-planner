package com.runplanner.health.dto;

public record HealthSyncResponse(
        int workoutsSaved,
        int workoutsSkipped,
        int workoutsMatched,
        int snapshotsSaved,
        boolean vdotUpdated
) {}
