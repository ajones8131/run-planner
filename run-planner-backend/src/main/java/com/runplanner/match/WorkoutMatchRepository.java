package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.workout.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutMatchRepository extends JpaRepository<WorkoutMatch, UUID> {

    Optional<WorkoutMatch> findByPlannedWorkout(PlannedWorkout plannedWorkout);

    Optional<WorkoutMatch> findByWorkout(Workout workout);

    boolean existsByPlannedWorkout(PlannedWorkout plannedWorkout);

    boolean existsByWorkout(Workout workout);

    List<WorkoutMatch> findByPlannedWorkoutIn(List<PlannedWorkout> plannedWorkouts);
}
