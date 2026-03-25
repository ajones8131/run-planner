package com.runplanner.vdot;

import com.runplanner.goalrace.GoalRaceRepository;
import com.runplanner.goalrace.GoalRaceStatus;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.runplanner.vdot.VdotConstants.*;

@Service
@RequiredArgsConstructor
public class VdotRecalculationService {

    private final VdotCalculator vdotCalculator;
    private final VdotHistoryService vdotHistoryService;
    private final GoalRaceRepository goalRaceRepository;

    @Transactional
    public Optional<VdotHistory> evaluate(Workout workout) {
        User user = workout.getUser();

        if (workout.getDurationSeconds() < MIN_DURATION_SECONDS) {
            return Optional.empty();
        }

        if (workout.getAvgHr() != null && user.getMaxHr() != null) {
            if (workout.getAvgHr() < user.getMaxHr() * HR_THRESHOLD_PERCENT) {
                return Optional.empty();
            }
        }

        if (!matchesRaceDistance(user, workout.getDistanceMeters())) {
            return Optional.empty();
        }

        double newVdot = vdotCalculator.calculateVdot(
                workout.getDistanceMeters(), workout.getDurationSeconds());
        double previousVdot = vdotHistoryService.getEffectiveVdot(user).orElse(0.0);

        return Optional.of(vdotHistoryService.recordCalculation(
                user, previousVdot, newVdot, workout.getId(), null));
    }

    private boolean matchesRaceDistance(User user, double distanceMeters) {
        List<Double> targetDistances = new ArrayList<>(List.of(
                DISTANCE_5K, DISTANCE_10K, DISTANCE_HALF_MARATHON, DISTANCE_MARATHON));

        goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE)
                .forEach(race -> targetDistances.add(race.getDistanceMeters().doubleValue()));

        return targetDistances.stream()
                .anyMatch(target -> Math.abs(distanceMeters - target) / target <= DISTANCE_TOLERANCE);
    }
}
