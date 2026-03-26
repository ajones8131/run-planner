package com.runplanner.adjustment;

import com.runplanner.match.WorkoutMatch;
import com.runplanner.match.WorkoutMatchRepository;
import com.runplanner.plan.*;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.runplanner.adjustment.AdjustmentConstants.*;

@Service
@RequiredArgsConstructor
public class PlanAdjustmentEngine {

    private final TrainingPlanRepository trainingPlanRepository;
    private final PlannedWorkoutRepository plannedWorkoutRepository;
    private final WorkoutMatchRepository workoutMatchRepository;
    private final VdotHistoryService vdotHistoryService;
    private final AdjustmentEvaluator adjustmentEvaluator;
    private final PlanAdjuster planAdjuster;
    private final Clock clock;

    @Transactional
    public AdjustmentDecision evaluate(User user) {
        var activePlan = trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE);
        if (activePlan.isEmpty()) {
            return AdjustmentDecision.none();
        }

        TrainingPlan plan = activePlan.get();
        LocalDate today = LocalDate.now(clock);

        LocalDate windowStart = today.minusDays(RECENT_MATCH_WINDOW_DAYS);
        List<PlannedWorkout> recentPlanned = plannedWorkoutRepository
                .findAllByTrainingPlanAndScheduledDateBetweenOrderByScheduledDate(
                        plan, windowStart, today.minusDays(1));

        List<WorkoutMatch> matches = recentPlanned.isEmpty()
                ? List.of()
                : workoutMatchRepository.findByPlannedWorkoutIn(recentPlanned);

        Map<PlannedWorkout, WorkoutMatch> matchMap = matches.stream()
                .collect(Collectors.toMap(WorkoutMatch::getPlannedWorkout, Function.identity()));

        List<MatchedWorkoutContext> matchedContexts = new ArrayList<>();
        List<PlannedWorkout> unmatchedLong = new ArrayList<>();

        for (PlannedWorkout pw : recentPlanned) {
            WorkoutMatch match = matchMap.get(pw);
            if (match != null) {
                matchedContexts.add(new MatchedWorkoutContext(match, pw, match.getWorkout()));
            } else if (pw.getWorkoutType() == WorkoutType.LONG
                    && pw.getScheduledDate().isAfter(today.minusDays(MISSED_LONG_RUN_WINDOW_DAYS + 1))) {
                unmatchedLong.add(pw);
            }
        }

        var effectiveVdot = vdotHistoryService.getEffectiveVdot(user);
        if (effectiveVdot.isEmpty()) {
            return AdjustmentDecision.none();
        }

        double currentVdot = effectiveVdot.get();
        double lastAdjVdot = plan.getLastAdjustmentVdot() != null
                ? plan.getLastAdjustmentVdot()
                : currentVdot;

        AdjustmentDecision decision = adjustmentEvaluator.evaluate(
                matchedContexts, unmatchedLong, currentVdot, lastAdjVdot);

        if (decision.type() != AdjustmentType.NONE) {
            planAdjuster.apply(plan, decision.type(), user);
            plan.setLastAdjustmentVdot(currentVdot);
            trainingPlanRepository.save(plan);
        }

        return decision;
    }
}
