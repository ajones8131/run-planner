package com.runplanner.plan;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceRepository;
import com.runplanner.plan.dto.PlannedWorkoutResponse;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final TrainingPlanGenerator trainingPlanGenerator;
    private final GoalRaceRepository goalRaceRepository;
    private final VdotHistoryService vdotHistoryService;

    @Transactional
    public TrainingPlanResponse generate(User user, UUID goalRaceId) {
        GoalRace race = goalRaceRepository.findByIdAndUser(goalRaceId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Goal race not found"));

        if (trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active plan already exists. Archive it before creating a new one.");
        }

        double vdot = vdotHistoryService.getEffectiveVdot(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No VDOT score available. Complete a qualifying workout first."));

        LocalDate startDate = LocalDate.now();
        TrainingPlan plan = trainingPlanRepository.save(TrainingPlan.builder()
                .user(user)
                .goalRace(race)
                .startDate(startDate)
                .endDate(race.getRaceDate())
                .build());

        List<PlannedWorkout> workouts = trainingPlanGenerator.generate(
                vdot, race.getDistanceMeters(), race.getRaceDate(), startDate);

        workouts.forEach(w -> w.setTrainingPlan(plan));
        plannedWorkoutRepository.saveAll(workouts);

        return TrainingPlanResponse.from(plan, workouts);
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse findActive(User user) {
        TrainingPlan plan = trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active training plan"));
        List<PlannedWorkout> workouts =
                plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan);
        return TrainingPlanResponse.from(plan, workouts);
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse findById(User user, UUID planId) {
        TrainingPlan plan = findOwnedPlan(user, planId);
        List<PlannedWorkout> workouts =
                plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan);
        return TrainingPlanResponse.from(plan, workouts);
    }

    @Transactional(readOnly = true)
    public List<TrainingPlanResponse> findAll(User user) {
        return trainingPlanRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(TrainingPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlannedWorkoutResponse> findPlannedWorkouts(User user, UUID planId,
                                                             LocalDate from, LocalDate to) {
        TrainingPlan plan = findOwnedPlan(user, planId);
        List<PlannedWorkout> workouts;
        if (from != null && to != null) {
            workouts = plannedWorkoutRepository
                    .findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(plan, from, to);
        } else {
            workouts = plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan);
        }
        return workouts.stream().map(PlannedWorkoutResponse::from).toList();
    }

    @Transactional
    public void archive(User user, UUID planId) {
        TrainingPlan plan = findOwnedPlan(user, planId);
        if (plan.getStatus() != TrainingPlanStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only ACTIVE plans can be archived");
        }
        plan.setStatus(TrainingPlanStatus.ARCHIVED);
        trainingPlanRepository.save(plan);
    }

    private TrainingPlan findOwnedPlan(User user, UUID planId) {
        return trainingPlanRepository.findByIdAndUser(planId, user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Training plan not found"));
    }
}
