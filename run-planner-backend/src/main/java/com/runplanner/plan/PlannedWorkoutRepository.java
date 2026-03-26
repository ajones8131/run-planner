package com.runplanner.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlannedWorkoutRepository extends JpaRepository<PlannedWorkout, UUID> {

    List<PlannedWorkout> findAllByTrainingPlanOrderByScheduledDate(TrainingPlan trainingPlan);

    List<PlannedWorkout> findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
            TrainingPlan trainingPlan, LocalDate from, LocalDate to);

    void deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(TrainingPlan trainingPlan, LocalDate date);

    List<PlannedWorkout> findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(
            TrainingPlan trainingPlan, LocalDate date);
}
