package com.runplanner.match;

import com.runplanner.plan.PlannedWorkout;
import com.runplanner.plan.WorkoutType;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ComplianceScorerTest {

    private final ComplianceScorer scorer = new ComplianceScorer();

    private User user(Integer maxHr) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("h")
                .maxHr(maxHr)
                .build();
    }

    private Workout workout(User user, double distanceMeters, int durationSeconds, Integer avgHr) {
        return Workout.builder()
                .id(UUID.randomUUID())
                .user(user)
                .source("TEST")
                .startedAt(Instant.now())
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .avgHr(avgHr)
                .build();
    }

    private PlannedWorkout planned(WorkoutType type, double distanceMeters,
                                    Double paceMin, Double paceMax, Integer hrZone) {
        return PlannedWorkout.builder()
                .id(UUID.randomUUID())
                .weekNumber(1)
                .dayOfWeek(2)
                .scheduledDate(LocalDate.of(2026, 3, 25))
                .workoutType(type)
                .targetDistanceMeters(distanceMeters)
                .targetPaceMinPerKm(paceMin)
                .targetPaceMaxPerKm(paceMax)
                .targetHrZone(hrZone)
                .originalScheduledDate(LocalDate.of(2026, 3, 25))
                .build();
    }

    @Test
    void score_restPlannedWorkout_returnsZero() {
        User user = user(180);
        Workout w = workout(user, 5000.0, 1500, 140);
        PlannedWorkout pw = planned(WorkoutType.REST, 0, null, null, null);

        double score = scorer.score(w, pw);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void score_perfectMatch_returnsOne() {
        User user = user(180);
        // Planned: 10km at 5.0-5.5 min/km, HR zone 1 (easy, 65-79% of 180 = 117-142)
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        // Midpoint pace 5.25 min/km for 10km: duration = 5.25 * 10 * 60 = 3150s
        Workout w = workout(user, 10000.0, 3150, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_distanceUnderTarget_proportionalFactor() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        // 8000m at 5.25 min/km: duration = 5.25 * 8 * 60 = 2520s
        Workout w = workout(user, 8000.0, 2520, 130);

        double score = scorer.score(w, pw);

        // Distance: 0.8*0.4=0.32, pace: ~1.0*0.4=0.4, HR: 1.0*0.2=0.2 → 0.92
        assertThat(score).isCloseTo(0.92, within(0.02));
    }

    @Test
    void score_distanceOverTarget_cappedAtOne() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        // 12000m at 5.25 min/km: 5.25 * 12 * 60 = 3780s
        Workout w = workout(user, 12000.0, 3780, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_paceAtMidpoint_fullPaceFactor() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        // Midpoint = 5.25 min/km for 10km: 3150s
        Workout w = workout(user, 10000.0, 3150, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_paceAtEdgeOfRange_halfPaceFactor() {
        User user = user(180);
        // Range: 5.0-5.5, width=0.5, mid=5.25
        // At 5.5 min/km: deviation = |5.5-5.25|/0.5 = 0.5 → pace factor = 0.5
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        // 5.5 min/km for 10km: 5.5*10*60 = 3300s
        Workout w = workout(user, 10000.0, 3300, 130);

        double score = scorer.score(w, pw);

        // 1.0*0.4 + 0.5*0.4 + 1.0*0.2 = 0.8
        assertThat(score).isCloseTo(0.8, within(0.02));
    }

    @Test
    void score_paceOutsideRange_zeroPaceFactor() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        // 7.0 min/km for 10km: 7.0*10*60 = 4200s (way too slow)
        Workout w = workout(user, 10000.0, 4200, 130);

        double score = scorer.score(w, pw);

        // 1.0*0.4 + 0.0*0.4 + 1.0*0.2 = 0.6
        assertThat(score).isCloseTo(0.6, within(0.02));
    }

    @Test
    void score_nullPlannedPaces_paceFactorDefaultsToOne() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, null, null, 1);
        Workout w = workout(user, 10000.0, 3150, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_zeroPaceRange_exactMatchIsOne() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.0, 1);
        // Exactly 5.0 min/km for 10km: 5.0*10*60 = 3000s
        Workout w = workout(user, 10000.0, 3000, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_zeroPaceRange_anyDeviationIsZero() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.0, 1);
        // 5.5 min/km (off from exact 5.0): 3300s
        Workout w = workout(user, 10000.0, 3300, 130);

        double score = scorer.score(w, pw);

        // 1.0*0.4 + 0.0*0.4 + 1.0*0.2 = 0.6
        assertThat(score).isCloseTo(0.6, within(0.02));
    }

    @Test
    void score_hrInTargetZone_fullHrFactor() {
        User user = user(180);
        // Zone 3 (Threshold): 86-90% of 180 = 155-162
        PlannedWorkout pw = planned(WorkoutType.THRESHOLD, 8000.0, 4.0, 4.3, 3);
        // Pace midpoint 4.15, for 8km: 4.15*8*60 = 1992s. avgHr 158 is in zone 3.
        Workout w = workout(user, 8000.0, 1992, 158);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.02));
    }

    @Test
    void score_hrOneZoneOff_halfHrFactor() {
        User user = user(180);
        // Zone 3 target. avgHr 150 is zone 2 (80-85% of 180 = 144-153)
        PlannedWorkout pw = planned(WorkoutType.THRESHOLD, 8000.0, 4.0, 4.3, 3);
        Workout w = workout(user, 8000.0, 1992, 150);

        double score = scorer.score(w, pw);

        // 0.4 + 0.4 + 0.5*0.2 = 0.9
        assertThat(score).isCloseTo(0.9, within(0.02));
    }

    @Test
    void score_hrTwoZonesOff_zeroHrFactor() {
        User user = user(180);
        // Zone 3 target. avgHr 130 is zone 1 (65-79% of 180 = 117-142)
        PlannedWorkout pw = planned(WorkoutType.THRESHOLD, 8000.0, 4.0, 4.3, 3);
        Workout w = workout(user, 8000.0, 1992, 130);

        double score = scorer.score(w, pw);

        // 0.4 + 0.4 + 0.0*0.2 = 0.8
        assertThat(score).isCloseTo(0.8, within(0.02));
    }

    @Test
    void score_nullAvgHr_hrFactorDefaultsToOne() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        Workout w = workout(user, 10000.0, 3150, null);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_nullUserMaxHr_hrFactorDefaultsToOne() {
        User user = user(null);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, 1);
        Workout w = workout(user, 10000.0, 3150, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }

    @Test
    void score_nullPlannedHrZone_hrFactorDefaultsToOne() {
        User user = user(180);
        PlannedWorkout pw = planned(WorkoutType.EASY, 10000.0, 5.0, 5.5, null);
        Workout w = workout(user, 10000.0, 3150, 130);

        double score = scorer.score(w, pw);

        assertThat(score).isCloseTo(1.0, within(0.01));
    }
}
