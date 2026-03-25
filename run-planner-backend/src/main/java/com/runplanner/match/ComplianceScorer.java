package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.springframework.stereotype.Component;

import static com.runplanner.match.MatchConstants.*;

@Component
public class ComplianceScorer {

    public double score(Workout workout, PlannedWorkout plannedWorkout) {
        if (plannedWorkout.getWorkoutType() == WorkoutType.REST) {
            return 0.0;
        }

        double distanceFactor = calculateDistanceFactor(workout, plannedWorkout);
        double paceFactor = calculatePaceFactor(workout, plannedWorkout);
        double hrFactor = calculateHrFactor(workout, plannedWorkout);

        return distanceFactor * DISTANCE_WEIGHT
                + paceFactor * PACE_WEIGHT
                + hrFactor * HR_WEIGHT;
    }

    private double calculateDistanceFactor(Workout workout, PlannedWorkout planned) {
        if (planned.getTargetDistanceMeters() <= 0) {
            return DEFAULT_FACTOR;
        }
        return Math.min(workout.getDistanceMeters() / planned.getTargetDistanceMeters(), 1.0);
    }

    private double calculatePaceFactor(Workout workout, PlannedWorkout planned) {
        if (planned.getTargetPaceMinPerKm() == null || planned.getTargetPaceMaxPerKm() == null) {
            return DEFAULT_FACTOR;
        }

        double actualPace = (workout.getDurationSeconds() / 60.0)
                / (workout.getDistanceMeters() / 1000.0);
        double targetMid = (planned.getTargetPaceMinPerKm() + planned.getTargetPaceMaxPerKm()) / 2.0;
        double targetRange = planned.getTargetPaceMaxPerKm() - planned.getTargetPaceMinPerKm();

        if (targetRange == 0) {
            return actualPace == targetMid ? 1.0 : 0.0;
        }

        double deviation = Math.abs(actualPace - targetMid) / targetRange;
        return Math.max(0.0, 1.0 - Math.min(deviation, 1.0));
    }

    private double calculateHrFactor(Workout workout, PlannedWorkout planned) {
        if (planned.getTargetHrZone() == null || workout.getAvgHr() == null) {
            return DEFAULT_FACTOR;
        }

        User user = workout.getUser();
        if (user.getMaxHr() == null) {
            return DEFAULT_FACTOR;
        }

        int actualZone = determineHrZone(workout.getAvgHr(), user.getMaxHr());
        int targetZone = planned.getTargetHrZone();
        int zoneDiff = Math.abs(actualZone - targetZone);

        if (zoneDiff == 0) return HR_ZONE_MATCH;
        if (zoneDiff == 1) return HR_ZONE_ADJACENT;
        return HR_ZONE_FAR;
    }

    int determineHrZone(int avgHr, int maxHr) {
        double pctMaxHr = (double) avgHr / maxHr;
        for (int i = HR_ZONE_LOWER_BOUNDS.length - 1; i >= 0; i--) {
            if (pctMaxHr >= HR_ZONE_LOWER_BOUNDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }
}
