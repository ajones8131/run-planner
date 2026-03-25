package com.runplanner.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record HealthSyncRequest(
        @Valid List<WorkoutSyncItem> workouts,
        @Valid List<HealthSnapshotSyncItem> healthSnapshots
) {
    public record WorkoutSyncItem(
            @NotBlank String source,
            String sourceId,
            @NotNull Instant startedAt,
            @NotNull Double distanceMeters,
            @NotNull Integer durationSeconds,
            Integer avgHr,
            Integer maxHr,
            Double elevationGain
    ) {}

    public record HealthSnapshotSyncItem(
            Double vo2maxEstimate,
            Integer restingHr,
            @NotNull Instant recordedAt
    ) {}
}
