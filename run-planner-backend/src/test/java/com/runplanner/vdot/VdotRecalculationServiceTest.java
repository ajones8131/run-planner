package com.runplanner.vdot;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceRepository;
import com.runplanner.goalrace.GoalRaceStatus;
import com.runplanner.user.User;
import com.runplanner.workout.Workout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VdotRecalculationServiceTest {

    @Mock private VdotCalculator vdotCalculator;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private GoalRaceRepository goalRaceRepository;

    @InjectMocks private VdotRecalculationService service;

    private User user(Integer maxHr) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("hashed")
                .maxHr(maxHr)
                .build();
    }

    private Workout workout(User user, double distanceMeters, int durationSeconds,
                            Integer avgHr) {
        return Workout.builder()
                .id(UUID.randomUUID())
                .user(user)
                .source("APPLE_HEALTH")
                .startedAt(Instant.now())
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .avgHr(avgHr)
                .build();
    }

    // --- Duration gate ---

    @Test
    void evaluate_durationBelowMinimum_returnsEmpty() {
        User user = user(180);
        Workout w = workout(user, 5000.0, 599, 170);

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isEmpty();
        verifyNoInteractions(vdotCalculator);
    }

    @Test
    void evaluate_durationExactlyAtMinimum_proceedsPastDurationGate() {
        User user = user(180);
        Workout w = workout(user, 5000.0, 600, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(5000.0, 600)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(48.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    // --- HR gate ---

    @Test
    void evaluate_avgHrNull_skipsHrGateAndProceeds() {
        User user = user(180);
        Workout w = workout(user, 5000.0, 1200, null);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(5000.0, 1200)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(48.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    @Test
    void evaluate_userMaxHrNull_skipsHrGateAndProceeds() {
        User user = user(null);
        Workout w = workout(user, 5000.0, 1200, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(5000.0, 1200)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(48.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    @Test
    void evaluate_avgHrBelowThreshold_returnsEmpty() {
        User user = user(180);
        // 90% of 180 = 162; avgHr 161 is below threshold
        Workout w = workout(user, 5000.0, 1200, 161);

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isEmpty();
        verifyNoInteractions(vdotCalculator);
    }

    @Test
    void evaluate_avgHrExactlyAtThreshold_proceedsPastHrGate() {
        User user = user(180);
        // 90% of 180 = 162
        Workout w = workout(user, 5000.0, 1200, 162);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(5000.0, 1200)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(48.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    // --- Distance gate ---

    @Test
    void evaluate_distanceMatchesStandard5k_triggers() {
        User user = user(180);
        Workout w = workout(user, 5000.0, 1200, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(5000.0, 1200)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());
        when(vdotHistoryService.recordCalculation(eq(user), eq(0.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    @Test
    void evaluate_distanceWithin5PercentOfHalfMarathon_triggers() {
        User user = user(180);
        Workout w = workout(user, 21000.0, 5400, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(21000.0, 5400)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(48.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    @Test
    void evaluate_distanceNotNearAnyStandard_returnsEmpty() {
        User user = user(180);
        Workout w = workout(user, 8000.0, 2400, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isEmpty();
        verifyNoInteractions(vdotCalculator);
    }

    @Test
    void evaluate_distanceMatchesActiveGoalRace_triggers() {
        User user = user(180);
        Workout w = workout(user, 15000.0, 3600, 170);
        GoalRace goalRace = GoalRace.builder()
                .distanceMeters(15000)
                .status(GoalRaceStatus.ACTIVE)
                .build();
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of(goalRace));
        when(vdotCalculator.calculateVdot(15000.0, 3600)).thenReturn(50.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(48.0), eq(50.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
    }

    // --- VDOT recording ---

    @Test
    void evaluate_noPreviousVdot_recordsWithZeroPrevious() {
        User user = user(180);
        Workout w = workout(user, 10000.0, 2400, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(10000.0, 2400)).thenReturn(52.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());
        when(vdotHistoryService.recordCalculation(eq(user), eq(0.0), eq(52.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(52.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
        verify(vdotHistoryService).recordCalculation(user, 0.0, 52.0, w.getId(), null);
    }

    @Test
    void evaluate_withPreviousVdot_recordsWithPreviousValue() {
        User user = user(180);
        Workout w = workout(user, 10000.0, 2400, 170);
        when(goalRaceRepository.findAllByUserAndStatus(user, GoalRaceStatus.ACTIVE))
                .thenReturn(List.of());
        when(vdotCalculator.calculateVdot(10000.0, 2400)).thenReturn(52.0);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(50.0), eq(52.0), eq(w.getId()), isNull()))
                .thenReturn(VdotHistory.builder().newVdot(52.0).build());

        Optional<VdotHistory> result = service.evaluate(w);

        assertThat(result).isPresent();
        verify(vdotHistoryService).recordCalculation(user, 50.0, 52.0, w.getId(), null);
    }
}
