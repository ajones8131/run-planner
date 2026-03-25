package com.runplanner.vdot;

import java.util.Map;

public record VdotScore(double score, Map<TrainingZone, PaceRange> trainingPaces) {

    public VdotScore {
        if (score < VdotConstants.MIN_VDOT || score > VdotConstants.MAX_VDOT) {
            throw new IllegalArgumentException(
                    "VDOT score must be between " + VdotConstants.MIN_VDOT
                    + " and " + VdotConstants.MAX_VDOT);
        }
        if (trainingPaces == null || trainingPaces.size() != TrainingZone.values().length) {
            throw new IllegalArgumentException("Training paces must include all zones");
        }
    }
}
