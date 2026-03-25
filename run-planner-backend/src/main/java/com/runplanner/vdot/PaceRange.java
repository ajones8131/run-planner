package com.runplanner.vdot;

public record PaceRange(double minPaceMinPerKm, double maxPaceMinPerKm) {

    public PaceRange {
        if (minPaceMinPerKm <= 0 || maxPaceMinPerKm <= 0) {
            throw new IllegalArgumentException("Pace values must be positive");
        }
        if (minPaceMinPerKm > maxPaceMinPerKm) {
            throw new IllegalArgumentException(
                    "minPace (faster) must be <= maxPace (slower)");
        }
    }
}
