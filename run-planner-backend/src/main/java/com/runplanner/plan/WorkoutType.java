package com.runplanner.plan;

import com.runplanner.vdot.TrainingZone;

public enum WorkoutType {

    EASY(TrainingZone.E),
    LONG(TrainingZone.E),
    MARATHON(TrainingZone.M),
    THRESHOLD(TrainingZone.T),
    INTERVAL(TrainingZone.I),
    REPETITION(TrainingZone.R),
    REST(null);

    private final TrainingZone trainingZone;

    WorkoutType(TrainingZone trainingZone) {
        this.trainingZone = trainingZone;
    }

    public TrainingZone getTrainingZone() {
        return trainingZone;
    }
}
