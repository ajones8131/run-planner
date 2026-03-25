package com.runplanner.vdot;

import java.time.Instant;
import java.util.UUID;

public record VdotHistoryResponse(
        UUID id,
        UUID triggeringWorkoutId,
        UUID triggeringSnapshotId,
        double previousVdot,
        double newVdot,
        Instant calculatedAt,
        boolean flagged,
        boolean accepted
) {
    public static VdotHistoryResponse from(VdotHistory entry) {
        return new VdotHistoryResponse(
                entry.getId(),
                entry.getTriggeringWorkoutId(),
                entry.getTriggeringSnapshotId(),
                entry.getPreviousVdot(),
                entry.getNewVdot(),
                entry.getCalculatedAt(),
                entry.isFlagged(),
                entry.isAccepted()
        );
    }
}
