package com.runplanner.adjustment;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.workout.Workout;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.runplanner.adjustment.AdjustmentConstants.*;

@Component
public class AdjustmentEvaluator {

    public AdjustmentDecision evaluate(List<MatchedWorkoutContext> recentMatches,
                                        List<PlannedWorkout> recentUnmatched,
                                        double currentVdot,
                                        double lastAdjustmentVdot) {

        if (hasConsecutiveLowCompliance(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "Consecutive workouts below compliance threshold");
        }

        if (hasMissedLongRun(recentUnmatched)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR, "Missed long run");
        }

        if (hasSignificantVdotChange(currentVdot, lastAdjustmentVdot)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "VDOT changed by more than " + VDOT_CHANGE_THRESHOLD + " points");
        }

        if (hasConsecutiveOverPerformance(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MAJOR,
                    "Consecutive workouts significantly faster than target");
        }

        if (hasUnderPerformanceDrift(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MINOR,
                    "Recent compliance trending below optimal range");
        }

        if (hasOverPerformanceDrift(recentMatches)) {
            return new AdjustmentDecision(AdjustmentType.MINOR,
                    "Recent pace consistently faster than target range");
        }

        return AdjustmentDecision.none();
    }

    private boolean hasConsecutiveLowCompliance(List<MatchedWorkoutContext> matches) {
        int consecutive = 0;
        for (MatchedWorkoutContext ctx : matches) {
            if (ctx.match().getComplianceScore() < MAJOR_LOW_COMPLIANCE_THRESHOLD) {
                consecutive++;
                if (consecutive >= MAJOR_CONSECUTIVE_COUNT) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    private boolean hasMissedLongRun(List<PlannedWorkout> unmatched) {
        return unmatched.stream()
                .anyMatch(pw -> pw.getWorkoutType() == WorkoutType.LONG);
    }

    private boolean hasSignificantVdotChange(double currentVdot, double lastAdjustmentVdot) {
        return Math.abs(currentVdot - lastAdjustmentVdot) > VDOT_CHANGE_THRESHOLD;
    }

    private boolean hasConsecutiveOverPerformance(List<MatchedWorkoutContext> matches) {
        int consecutive = 0;
        for (MatchedWorkoutContext ctx : matches) {
            if (isOverPerforming(ctx)) {
                consecutive++;
                if (consecutive >= MAJOR_CONSECUTIVE_COUNT) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    private boolean isOverPerforming(MatchedWorkoutContext ctx) {
        PlannedWorkout planned = ctx.planned();
        if (planned.getTargetPaceMinPerKm() == null || planned.getTargetPaceMaxPerKm() == null) {
            return false;
        }
        double midpoint = (planned.getTargetPaceMinPerKm() + planned.getTargetPaceMaxPerKm()) / 2.0;
        double actualPace = calculateActualPace(ctx.actual());
        return actualPace < midpoint * OVER_PERFORMANCE_PACE_FACTOR;
    }

    private boolean hasUnderPerformanceDrift(List<MatchedWorkoutContext> matches) {
        List<MatchedWorkoutContext> window = lastN(matches, MINOR_WINDOW_SIZE);
        if (window.size() < MINOR_WINDOW_SIZE) return false;
        long count = window.stream()
                .filter(ctx -> {
                    double score = ctx.match().getComplianceScore();
                    return score >= MINOR_COMPLIANCE_LOW && score <= MINOR_COMPLIANCE_HIGH;
                })
                .count();
        return count >= MINOR_TRIGGER_COUNT;
    }

    private boolean hasOverPerformanceDrift(List<MatchedWorkoutContext> matches) {
        List<MatchedWorkoutContext> window = lastN(matches, MINOR_WINDOW_SIZE);
        if (window.size() < MINOR_WINDOW_SIZE) return false;
        long count = window.stream()
                .filter(ctx -> {
                    PlannedWorkout planned = ctx.planned();
                    if (planned.getTargetPaceMinPerKm() == null) return false;
                    double actualPace = calculateActualPace(ctx.actual());
                    return actualPace < planned.getTargetPaceMinPerKm();
                })
                .count();
        return count >= MINOR_TRIGGER_COUNT;
    }

    private double calculateActualPace(Workout workout) {
        return (workout.getDurationSeconds() / 60.0) / (workout.getDistanceMeters() / 1000.0);
    }

    private <T> List<T> lastN(List<T> list, int n) {
        if (list.size() <= n) return list;
        return list.subList(list.size() - n, list.size());
    }
}
