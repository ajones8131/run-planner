package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatchRepository;
import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAdjustmentEngineTest {

    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private WorkoutMatchRepository workoutMatchRepository;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private AdjustmentEvaluator adjustmentEvaluator;
    @Mock private PlanAdjuster planAdjuster;
    @Mock private Clock clock;

    @InjectMocks private PlanAdjustmentEngine engine;

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-03-25T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("t@t.com").passwordHash("h").build();
    }

    private TrainingPlan plan(User user, Double lastAdjVdot) {
        return TrainingPlan.builder()
                .id(UUID.randomUUID()).user(user)
                .status(TrainingPlanStatus.ACTIVE)
                .lastAdjustmentVdot(lastAdjVdot)
                .build();
    }

    @Test
    void evaluate_noActivePlan_returnsNone() {
        User user = user();
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
        verify(planAdjuster, never()).apply(any(), any(), any());
    }

    @Test
    void evaluate_noVdot_returnsNone() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, 50.0);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
        verify(planAdjuster, never()).apply(any(), any(), any());
    }

    @Test
    void evaluate_evaluatorReturnsMajor_adjusterCalled() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, 50.0);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(53.0));
        when(adjustmentEvaluator.evaluate(anyList(), anyList(), eq(53.0), eq(50.0)))
                .thenReturn(new AdjustmentDecision(AdjustmentType.MAJOR, "VDOT changed"));

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.MAJOR);
        verify(planAdjuster).apply(p, AdjustmentType.MAJOR, user);
        assertThat(p.getLastAdjustmentVdot()).isEqualTo(53.0);
    }

    @Test
    void evaluate_evaluatorReturnsNone_adjusterNotCalled() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, 50.0);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(adjustmentEvaluator.evaluate(anyList(), anyList(), eq(50.0), eq(50.0)))
                .thenReturn(AdjustmentDecision.none());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
        verify(planAdjuster, never()).apply(any(), any(), any());
    }

    @Test
    void evaluate_nullLastAdjustmentVdot_usesCurrentVdot() {
        stubClock();
        User user = user();
        TrainingPlan p = plan(user, null);
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(p));
        when(plannedWorkoutRepository.findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                eq(p), any(), any())).thenReturn(List.of());
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(adjustmentEvaluator.evaluate(anyList(), anyList(), eq(50.0), eq(50.0)))
                .thenReturn(AdjustmentDecision.none());

        AdjustmentDecision decision = engine.evaluate(user);

        assertThat(decision.type()).isEqualTo(AdjustmentType.NONE);
    }
}
