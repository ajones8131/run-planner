package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdjustmentEvaluatorTest {

    private final AdjustmentEvaluator evaluator = new AdjustmentEvaluator();

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("t@t.com").passwordHash("h").build();
    }

    private MatchedWorkoutContext matched(double complianceScore, double actualPaceMinPerKm,
                                           Double plannedPaceMin, Double plannedPaceMax,
                                           LocalDate scheduledDate) {
        User u = user();
        double distanceMeters = 10000.0;
        int durationSeconds = (int) (actualPaceMinPerKm * (distanceMeters / 1000.0) * 60);

        PlannedWorkout planned = PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .scheduledDate(scheduledDate)
                .workoutType(WorkoutType.EASY)
                .targetDistanceMeters(distanceMeters)
                .targetPaceMinPerKm(plannedPaceMin)
                .targetPaceMaxPerKm(plannedPaceMax)
                .originalScheduledDate(scheduledDate)
                .build();

        Workout actual = Workout.builder()
                .id(UUID.randomUUID())
                .user(u)
                .source("TEST")
                .startedAt(scheduledDate.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant())
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .build();

        WorkoutMatch match = WorkoutMatch.builder()
                .id(UUID.randomUUID())
                .plannedWorkout(planned)
                .workout(actual)
                .complianceScore(complianceScore)
                .build();

        return new MatchedWorkoutContext(match, planned, actual);
    }

    private PlannedWorkout unmatchedLong(LocalDate date) {
        return PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .scheduledDate(date)
                .workoutType(WorkoutType.LONG)
                .targetDistanceMeters(15000.0)
                .originalScheduledDate(date)
                .build();
    }

    @Test
    void evaluate_noMatches_returnsNone() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }

    @Test
    void evaluate_twoConsecutiveLowCompliance_returnsMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        var matches = List.of(
                matched(0.5, 5.25, 5.0, 5.5, d1),
                matched(0.4, 5.25, 5.0, 5.5, d2));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_oneHighOneLow_notConsecutive_notMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        LocalDate d3 = LocalDate.of(2026, 3, 25);
        var matches = List.of(
                matched(0.5, 5.25, 5.0, 5.5, d1),
                matched(0.8, 5.25, 5.0, 5.5, d2),
                matched(0.5, 5.25, 5.0, 5.5, d3));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_missedLongRun_returnsMajor() {
        LocalDate recent = LocalDate.of(2026, 3, 22);
        var unmatched = List.of(unmatchedLong(recent));

        AdjustmentDecision decision = evaluator.evaluate(List.of(), unmatched, 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_vdotIncreasedMoreThan2_returnsMajor() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 53.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_vdotDecreasedMoreThan2_returnsMajor() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 47.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_vdotChangedExactly2_notMajor() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 52.0, 50.0);
        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_twoConsecutiveOverPerformance_returnsMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        // Midpoint = 5.25, 10% faster = 4.725. Actual pace 4.5 < 4.725
        var matches = List.of(
                matched(0.8, 4.5, 5.0, 5.5, d1),
                matched(0.8, 4.5, 5.0, 5.5, d2));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_overPerformanceNotConsecutive_noMajor() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        LocalDate d3 = LocalDate.of(2026, 3, 25);
        var matches = List.of(
                matched(0.8, 4.5, 5.0, 5.5, d1),
                matched(0.8, 5.25, 5.0, 5.5, d2),
                matched(0.8, 4.5, 5.0, 5.5, d3));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_overPerformanceNullPaces_skipped() {
        LocalDate d1 = LocalDate.of(2026, 3, 23);
        LocalDate d2 = LocalDate.of(2026, 3, 24);
        var matches = List.of(
                matched(0.8, 4.5, null, null, d1),
                matched(0.8, 4.5, null, null, d2));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isNotEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_threeOfFiveMediumCompliance_returnsMinor() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        var matches = List.of(
                matched(0.65, 5.25, 5.0, 5.5, base),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.7, 5.25, 5.0, 5.5, base.plusDays(2)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.72, 5.25, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MINOR);
    }

    @Test
    void evaluate_twoOfFiveMediumCompliance_returnsNone() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        var matches = List.of(
                matched(0.65, 5.25, 5.0, 5.5, base),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.7, 5.25, 5.0, 5.5, base.plusDays(2)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }

    @Test
    void evaluate_threeOfFiveFasterThanPaceMin_returnsMinor() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        // paceMin = 5.0, actual 4.8 < 5.0 → faster than fast end
        var matches = List.of(
                matched(0.9, 4.8, 5.0, 5.5, base),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.9, 4.8, 5.0, 5.5, base.plusDays(2)),
                matched(0.9, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.9, 4.8, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MINOR);
    }

    @Test
    void evaluate_majorTakesPrecedenceOverMinor() {
        LocalDate base = LocalDate.of(2026, 3, 20);
        var matches = List.of(
                matched(0.65, 5.25, 5.0, 5.5, base),
                matched(0.65, 5.25, 5.0, 5.5, base.plusDays(1)),
                matched(0.5, 5.25, 5.0, 5.5, base.plusDays(2)),
                matched(0.5, 5.25, 5.0, 5.5, base.plusDays(3)),
                matched(0.65, 5.25, 5.0, 5.5, base.plusDays(4)));

        AdjustmentDecision decision = evaluator.evaluate(matches, List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
    }

    @Test
    void evaluate_emptyMatchList_returnsNone() {
        AdjustmentDecision decision = evaluator.evaluate(List.of(), List.of(), 50.0, 50.0);
        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }
}
