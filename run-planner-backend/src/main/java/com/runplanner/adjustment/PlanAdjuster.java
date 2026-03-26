package com.runplanner.adjustment;

import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import com.runplanner.vdot.VdotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanAdjuster {

    private final TrainingPlanGenerator trainingPlanGenerator;
    private final TrainingPaceCalculator trainingPaceCalculator;
    private final VdotHistoryService vdotHistoryService;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final Clock clock;

    public void apply(TrainingPlan plan, AdjustmentType type, User user) {
        double vdot = vdotHistoryService.getEffectiveVdot(user).orElseThrow();
        LocalDate today = LocalDate.now(clock);

        if (type == AdjustmentType.MAJOR) {
            applyMajor(plan, vdot, today);
        } else if (type == AdjustmentType.MINOR) {
            applyMinor(plan, vdot, today);
        }

        plan.setRevision(plan.getRevision() + 1);
        trainingPlanRepository.save(plan);
    }

    private void applyMajor(TrainingPlan plan, double vdot, LocalDate today) {
        plannedWorkoutRepository.deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(plan, today);

        List<PlannedWorkout> newWorkouts = trainingPlanGenerator.generate(
                vdot, plan.getGoalRace().getDistanceMeters(), plan.getEndDate(), today);

        int newRevision = plan.getRevision() + 1;
        newWorkouts.forEach(w -> {
            w.setTrainingPlan(plan);
            w.setPlanRevision(newRevision);
        });

        plannedWorkoutRepository.saveAll(newWorkouts);
    }

    private void applyMinor(TrainingPlan plan, double vdot, LocalDate today) {
        Map<TrainingZone, PaceRange> paces = trainingPaceCalculator.calculate(vdot);

        List<PlannedWorkout> futureWorkouts = plannedWorkoutRepository
                .findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(plan, today);

        for (PlannedWorkout workout : futureWorkouts) {
            TrainingZone zone = workout.getWorkoutType().getTrainingZone();
            if (zone == null) continue;

            PaceRange pace = paces.get(zone);
            workout.setTargetPaceMinPerKm(pace.minPaceMinPerKm());
            workout.setTargetPaceMaxPerKm(pace.maxPaceMinPerKm());
        }

        plannedWorkoutRepository.saveAll(futureWorkouts);
    }
}
