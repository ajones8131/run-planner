package com.runplanner.plan;

import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingPlanGeneratorTest {

    @Mock private TrainingPaceCalculator trainingPaceCalculator;

    @InjectMocks private TrainingPlanGenerator generator;

    private static final double VDOT = 50.0;
    private static final int RACE_DISTANCE_METERS = 21097;

    private Map<TrainingZone, PaceRange> samplePaces() {
        return Map.of(
                TrainingZone.E, new PaceRange(5.0, 6.0),
                TrainingZone.M, new PaceRange(4.5, 4.7),
                TrainingZone.T, new PaceRange(4.0, 4.3),
                TrainingZone.I, new PaceRange(3.5, 3.8),
                TrainingZone.R, new PaceRange(3.0, 3.3)
        );
    }

    private void stubPaces() {
        when(trainingPaceCalculator.calculate(anyDouble())).thenReturn(samplePaces());
    }

    @Test
    void generate_12weeks_produces84days() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5); // Monday
        LocalDate raceDate = LocalDate.of(2026, 3, 30); // 12 weeks later

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        assertThat(workouts).hasSize(84);
    }

    @Test
    void generate_allDatesAreWithinStartAndEndRange() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.forEach(w -> {
            assertThat(w.getScheduledDate()).isAfterOrEqualTo(start);
            assertThat(w.getScheduledDate()).isBefore(raceDate);
        });
    }

    @Test
    void generate_originalScheduledDate_equalsScheduledDate() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.forEach(w ->
                assertThat(w.getOriginalScheduledDate()).isEqualTo(w.getScheduledDate()));
    }

    @Test
    void generate_allPlanRevisions_areOne() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.forEach(w -> assertThat(w.getPlanRevision()).isEqualTo(1));
    }

    @Test
    void generate_dayOfWeek_matchesScheduledDate() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.forEach(w ->
                assertThat(w.getDayOfWeek()).isEqualTo(w.getScheduledDate().getDayOfWeek().getValue()));
    }

    @Test
    void generate_12weeks_assignsPhasesEvenly() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        // Week 1 Tue (base) = EASY
        PlannedWorkout week1Tue = workouts.get(1);
        assertThat(week1Tue.getWorkoutType()).isEqualTo(WorkoutType.EASY);

        // Week 4 Tue (quality) = THRESHOLD
        PlannedWorkout week4Tue = workouts.get(3 * 7 + 1);
        assertThat(week4Tue.getWorkoutType()).isEqualTo(WorkoutType.THRESHOLD);

        // Week 7 Tue (peak) = INTERVAL
        PlannedWorkout week7Tue = workouts.get(6 * 7 + 1);
        assertThat(week7Tue.getWorkoutType()).isEqualTo(WorkoutType.INTERVAL);

        // Week 10 Tue (taper) = EASY
        PlannedWorkout week10Tue = workouts.get(9 * 7 + 1);
        assertThat(week10Tue.getWorkoutType()).isEqualTo(WorkoutType.EASY);
    }

    @Test
    void generate_shortPlan_3weeks_skipBaseAndQuality() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 3, 9);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        assertThat(workouts).hasSize(21);

        // Week 1 Tue = INTERVAL (peak template)
        PlannedWorkout week1Tue = workouts.get(1);
        assertThat(week1Tue.getWorkoutType()).isEqualTo(WorkoutType.INTERVAL);
    }

    @Test
    void generate_mondaysAndFridays_areRestDays() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.stream()
                .filter(w -> w.getDayOfWeek() == 1 || w.getDayOfWeek() == 5)
                .forEach(w -> assertThat(w.getWorkoutType()).isEqualTo(WorkoutType.REST));
    }

    @Test
    void generate_saturdays_areLongRuns() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.stream()
                .filter(w -> w.getDayOfWeek() == 6)
                .forEach(w -> assertThat(w.getWorkoutType()).isEqualTo(WorkoutType.LONG));
    }

    @Test
    void generate_restDays_haveZeroDistanceAndNullPaces() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.stream()
                .filter(w -> w.getWorkoutType() == WorkoutType.REST)
                .forEach(w -> {
                    assertThat(w.getTargetDistanceMeters()).isEqualTo(0.0);
                    assertThat(w.getTargetPaceMinPerKm()).isNull();
                    assertThat(w.getTargetPaceMaxPerKm()).isNull();
                });
    }

    @Test
    void generate_nonRestDays_havePositiveDistanceAndPaces() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.stream()
                .filter(w -> w.getWorkoutType() != WorkoutType.REST)
                .forEach(w -> {
                    assertThat(w.getTargetDistanceMeters()).isGreaterThan(0);
                    assertThat(w.getTargetPaceMinPerKm()).isNotNull();
                    assertThat(w.getTargetPaceMaxPerKm()).isNotNull();
                });
    }

    @Test
    void generate_longRun_getsLargestDistanceInWeek() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        List<PlannedWorkout> week1 = workouts.subList(0, 7);
        PlannedWorkout longRun = week1.stream()
                .filter(w -> w.getWorkoutType() == WorkoutType.LONG)
                .findFirst().orElseThrow();
        double maxNonLong = week1.stream()
                .filter(w -> w.getWorkoutType() != WorkoutType.LONG && w.getWorkoutType() != WorkoutType.REST)
                .mapToDouble(PlannedWorkout::getTargetDistanceMeters)
                .max().orElse(0);

        assertThat(longRun.getTargetDistanceMeters()).isGreaterThan(maxNonLong);
    }

    @Test
    void generate_taperWeeks_haveLowerTotalDistanceThanPeakWeeks() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        double peakTotal = workouts.subList(42, 63).stream()
                .mapToDouble(PlannedWorkout::getTargetDistanceMeters).sum();
        double taperTotal = workouts.subList(63, 84).stream()
                .mapToDouble(PlannedWorkout::getTargetDistanceMeters).sum();

        assertThat(taperTotal).isLessThan(peakTotal);
    }

    @Test
    void generate_weekNumbers_areSequential() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        for (int i = 0; i < workouts.size(); i++) {
            int expectedWeek = (i / 7) + 1;
            assertThat(workouts.get(i).getWeekNumber()).isEqualTo(expectedWeek);
        }
    }

    @Test
    void generate_trainingPlan_isNull() {
        stubPaces();
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate raceDate = LocalDate.of(2026, 3, 30);

        List<PlannedWorkout> workouts = generator.generate(VDOT, RACE_DISTANCE_METERS, raceDate, start);

        workouts.forEach(w -> assertThat(w.getTrainingPlan()).isNull());
    }
}
