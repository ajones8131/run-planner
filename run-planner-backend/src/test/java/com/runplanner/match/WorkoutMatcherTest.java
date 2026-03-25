package com.runplanner.match;

import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutMatcherTest {

    @Mock private WorkoutMatchRepository workoutMatchRepository;
    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private ComplianceScorer complianceScorer;

    @InjectMocks private WorkoutMatcher workoutMatcher;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").maxHr(180).build();
    }

    private Workout workout(User user, LocalDate date) {
        return Workout.builder()
                .id(UUID.randomUUID())
                .user(user)
                .source("TEST")
                .startedAt(date.atStartOfDay().toInstant(ZoneOffset.UTC))
                .distanceMeters(10000.0)
                .durationSeconds(3000)
                .avgHr(150)
                .build();
    }

    private TrainingPlan activePlan(User user) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .status(TrainingPlanStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    private PlannedWorkout planned(LocalDate date, WorkoutType type) {
        return PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .scheduledDate(date)
                .workoutType(type)
                .targetDistanceMeters(10000.0)
                .targetPaceMinPerKm(5.0)
                .targetPaceMaxPerKm(5.5)
                .originalScheduledDate(date)
                .build();
    }

    @Test
    void match_happyPath_matchesBestCandidate() {
        User user = user();
        LocalDate date = LocalDate.of(2026, 3, 25);
        Workout w = workout(user, date);
        TrainingPlan plan = activePlan(user);
        PlannedWorkout pw = planned(date, WorkoutType.EASY);

        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
                .thenReturn(List.of(pw));
        when(workoutMatchRepository.existsByPlannedWorkout(pw)).thenReturn(false);
        when(complianceScorer.score(w, pw)).thenReturn(0.85);
        when(workoutMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isPresent();
        assertThat(result.get().getComplianceScore()).isEqualTo(0.85);
        verify(workoutMatchRepository).save(any());
    }

    @Test
    void match_alreadyMatched_returnsEmpty() {
        User user = user();
        Workout w = workout(user, LocalDate.of(2026, 3, 25));
        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(true);

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isEmpty();
        verify(trainingPlanRepository, never()).findByUserAndStatus(any(), any());
    }

    @Test
    void match_noActivePlan_returnsEmpty() {
        User user = user();
        Workout w = workout(user, LocalDate.of(2026, 3, 25));
        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isEmpty();
    }

    @Test
    void match_noCandidatesWithinWindow_returnsEmpty() {
        User user = user();
        LocalDate workoutDate = LocalDate.of(2026, 3, 25);
        Workout w = workout(user, workoutDate);
        TrainingPlan plan = activePlan(user);
        PlannedWorkout pw = planned(workoutDate.plusDays(3), WorkoutType.EASY);

        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
                .thenReturn(List.of(pw));

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isEmpty();
    }

    @Test
    void match_multipleCandidates_picksBestScore() {
        User user = user();
        LocalDate date = LocalDate.of(2026, 3, 25);
        Workout w = workout(user, date);
        TrainingPlan plan = activePlan(user);
        PlannedWorkout pw1 = planned(date, WorkoutType.EASY);
        PlannedWorkout pw2 = planned(date, WorkoutType.THRESHOLD);

        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
                .thenReturn(List.of(pw1, pw2));
        when(workoutMatchRepository.existsByPlannedWorkout(pw1)).thenReturn(false);
        when(workoutMatchRepository.existsByPlannedWorkout(pw2)).thenReturn(false);
        when(complianceScorer.score(w, pw1)).thenReturn(0.7);
        when(complianceScorer.score(w, pw2)).thenReturn(0.9);
        when(workoutMatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isPresent();
        assertThat(result.get().getPlannedWorkout()).isEqualTo(pw2);
        assertThat(result.get().getComplianceScore()).isEqualTo(0.9);
    }

    @Test
    void match_restPlannedWorkoutsExcluded() {
        User user = user();
        LocalDate date = LocalDate.of(2026, 3, 25);
        Workout w = workout(user, date);
        TrainingPlan plan = activePlan(user);
        PlannedWorkout restPw = planned(date, WorkoutType.REST);

        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
                .thenReturn(List.of(restPw));

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isEmpty();
    }

    @Test
    void match_alreadyMatchedPlannedWorkoutExcluded() {
        User user = user();
        LocalDate date = LocalDate.of(2026, 3, 25);
        Workout w = workout(user, date);
        TrainingPlan plan = activePlan(user);
        PlannedWorkout pw = planned(date, WorkoutType.EASY);

        when(workoutMatchRepository.existsByWorkout(w)).thenReturn(false);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
                .thenReturn(List.of(pw));
        when(workoutMatchRepository.existsByPlannedWorkout(pw)).thenReturn(true);

        Optional<WorkoutMatch> result = workoutMatcher.match(w);

        assertThat(result).isEmpty();
    }
}
