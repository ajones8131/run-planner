package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.plan.PlannedWorkout;
import com.runplanner.workout.Workout;

public record MatchedWorkoutContext(
        WorkoutMatch match,
        PlannedWorkout planned,
        Workout actual
) {}
