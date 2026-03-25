package com.runplanner.match;

import com.runplanner.plan.*;
import com.runplanner.workout.Workout;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;

import static com.runplanner.match.MatchConstants.MATCH_WINDOW_DAYS;

@Service
@RequiredArgsConstructor
public class WorkoutMatcher {

    private final WorkoutMatchRepository workoutMatchRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final ComplianceScorer complianceScorer;

    @Transactional
    public Optional<WorkoutMatch> match(Workout workout) {
        if (workoutMatchRepository.existsByWorkout(workout)) {
            return Optional.empty();
        }

        var activePlan = trainingPlanRepository.findByUserAndStatus(
                workout.getUser(), TrainingPlanStatus.ACTIVE);
        if (activePlan.isEmpty()) {
            return Optional.empty();
        }

        LocalDate workoutDate = workout.getStartedAt()
                .atZone(ZoneOffset.UTC).toLocalDate();

        var allPlanned = plannedWorkoutRepository
                .findAllByTrainingPlanOrderByScheduledDate(activePlan.get());

        record ScoredCandidate(PlannedWorkout planned, double score) {}

        var bestCandidate = allPlanned.stream()
                .filter(pw -> pw.getWorkoutType() != WorkoutType.REST)
                .filter(pw -> !workoutMatchRepository.existsByPlannedWorkout(pw))
                .filter(pw -> isWithinWindow(workoutDate, pw.getScheduledDate()))
                .map(pw -> new ScoredCandidate(pw, complianceScorer.score(workout, pw)))
                .max(Comparator.comparingDouble(ScoredCandidate::score));

        if (bestCandidate.isEmpty()) {
            return Optional.empty();
        }

        var match = WorkoutMatch.builder()
                .plannedWorkout(bestCandidate.get().planned())
                .workout(workout)
                .complianceScore(bestCandidate.get().score())
                .build();

        return Optional.of(workoutMatchRepository.save(match));
    }

    private boolean isWithinWindow(LocalDate workoutDate, LocalDate scheduledDate) {
        long daysBetween = Math.abs(workoutDate.toEpochDay() - scheduledDate.toEpochDay());
        return daysBetween <= MATCH_WINDOW_DAYS;
    }
}
