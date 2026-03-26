package com.runplanner.adjustment;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import com.runplanner.vdot.VdotHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAdjusterTest {

    @Mock private TrainingPlanGenerator trainingPlanGenerator;
    @Mock private TrainingPaceCalculator trainingPaceCalculator;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private Clock clock;

    @InjectMocks private PlanAdjuster adjuster;

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-03-25T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("t@t.com").passwordHash("h").build();
    }

    private TrainingPlan plan(User user) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .goalRace(GoalRace.builder().id(UUID.randomUUID()).distanceMeters(21097)
                        .raceDate(LocalDate.of(2026, 6, 1)).distanceLabel("HM").build())
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 6, 1))
                .status(TrainingPlanStatus.ACTIVE)
                .revision(1)
                .build();
    }

    @Test
    void apply_major_deletesFutureAndRegenerates() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(trainingPlanGenerator.generate(eq(50.0), eq(21097), eq(p.getEndDate()), eq(LocalDate.of(2026, 3, 25))))
                .thenReturn(List.of(
                        PlannedWorkout.builder().weekNumber(1).dayOfWeek(1)
                                .scheduledDate(LocalDate.of(2026, 3, 25))
                                .workoutType(WorkoutType.REST).targetDistanceMeters(0)
                                .originalScheduledDate(LocalDate.of(2026, 3, 25)).build()));
        when(plannedWorkoutRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(trainingPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adjuster.apply(p, AdjustmentType.MAJOR, user);

        verify(plannedWorkoutRepository).deleteAllByTrainingPlanAndScheduledDateGreaterThanEqual(
                p, LocalDate.of(2026, 3, 25));
        verify(trainingPlanGenerator).generate(50.0, 21097, p.getEndDate(), LocalDate.of(2026, 3, 25));
        verify(plannedWorkoutRepository).saveAll(anyList());
        assertThat(p.getRevision()).isEqualTo(2);
    }

    @Test
    void apply_minor_updatesPacesOnFutureWorkouts() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(52.0));
        Map<TrainingZone, PaceRange> paces = Map.of(
                TrainingZone.E, new PaceRange(4.8, 5.8),
                TrainingZone.M, new PaceRange(4.3, 4.5),
                TrainingZone.T, new PaceRange(3.8, 4.1),
                TrainingZone.I, new PaceRange(3.3, 3.6),
                TrainingZone.R, new PaceRange(2.8, 3.1));
        when(trainingPaceCalculator.calculate(52.0)).thenReturn(paces);

        PlannedWorkout easyWorkout = PlannedWorkout.builder()
                .id(UUID.randomUUID()).trainingPlan(p)
                .scheduledDate(LocalDate.of(2026, 3, 26))
                .workoutType(WorkoutType.EASY)
                .targetDistanceMeters(10000).targetPaceMinPerKm(5.0).targetPaceMaxPerKm(6.0)
                .originalScheduledDate(LocalDate.of(2026, 3, 26)).build();
        PlannedWorkout restWorkout = PlannedWorkout.builder()
                .id(UUID.randomUUID()).trainingPlan(p)
                .scheduledDate(LocalDate.of(2026, 3, 27))
                .workoutType(WorkoutType.REST)
                .targetDistanceMeters(0)
                .originalScheduledDate(LocalDate.of(2026, 3, 27)).build();

        when(plannedWorkoutRepository
                .findAllByTrainingPlanAndScheduledDateGreaterThanEqualOrderByScheduledDate(
                        p, LocalDate.of(2026, 3, 25)))
                .thenReturn(List.of(easyWorkout, restWorkout));
        when(plannedWorkoutRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(trainingPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adjuster.apply(p, AdjustmentType.MINOR, user);

        assertThat(easyWorkout.getTargetPaceMinPerKm()).isEqualTo(4.8);
        assertThat(easyWorkout.getTargetPaceMaxPerKm()).isEqualTo(5.8);
        assertThat(restWorkout.getTargetPaceMinPerKm()).isNull();
        assertThat(p.getRevision()).isEqualTo(2);
    }
}
