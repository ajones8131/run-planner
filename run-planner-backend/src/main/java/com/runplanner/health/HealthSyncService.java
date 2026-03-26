package com.runplanner.health;

import com.runplanner.adjustment.AdjustmentDecision;
import com.runplanner.adjustment.PlanAdjustmentEngine;
import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncRequest.HealthSnapshotSyncItem;
import com.runplanner.health.dto.HealthSyncRequest.WorkoutSyncItem;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.match.WorkoutMatcher;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import com.runplanner.vdot.VdotHistoryService;
import com.runplanner.workout.Workout;
import com.runplanner.workout.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthSyncService {

    private final WorkoutService workoutService;
    private final WorkoutMatcher workoutMatcher;
    private final HealthSnapshotRepository healthSnapshotRepository;
    private final VdotHistoryService vdotHistoryService;
    private final UserRepository userRepository;
    private final PlanAdjustmentEngine planAdjustmentEngine;

    @Transactional
    public HealthSyncResponse sync(User user, HealthSyncRequest request) {
        int workoutsSaved = 0;
        int workoutsSkipped = 0;
        int workoutsMatched = 0;
        int snapshotsSaved = 0;
        boolean vdotUpdated = false;

        // 1. Ingest workouts
        List<Workout> newWorkouts = new ArrayList<>();
        if (request.workouts() != null) {
            for (WorkoutSyncItem item : request.workouts()) {
                if (item.sourceId() != null
                        && workoutService.existsBySourceAndSourceId(item.source(), item.sourceId())) {
                    workoutsSkipped++;
                    continue;
                }

                Workout workout = Workout.builder()
                        .user(user)
                        .source(item.source())
                        .sourceId(item.sourceId())
                        .startedAt(item.startedAt())
                        .distanceMeters(item.distanceMeters())
                        .durationSeconds(item.durationSeconds())
                        .avgHr(item.avgHr())
                        .maxHr(item.maxHr())
                        .elevationGain(item.elevationGain())
                        .build();

                Workout saved = workoutService.save(workout);
                newWorkouts.add(saved);
                workoutsSaved++;
            }
        }

        // 2. Match new workouts
        for (Workout workout : newWorkouts) {
            if (workoutMatcher.match(workout).isPresent()) {
                workoutsMatched++;
            }
        }

        // 3. Ingest health snapshots
        if (request.healthSnapshots() != null) {
            for (HealthSnapshotSyncItem item : request.healthSnapshots()) {
                HealthSnapshot snapshot = HealthSnapshot.builder()
                        .user(user)
                        .vo2maxEstimate(item.vo2maxEstimate())
                        .restingHr(item.restingHr())
                        .recordedAt(item.recordedAt())
                        .build();
                healthSnapshotRepository.save(snapshot);
                snapshotsSaved++;
            }
        }

        // 4. VDOT from VO2max
        var latestVo2max = healthSnapshotRepository
                .findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user);
        if (latestVo2max.isPresent()) {
            double newVdot = latestVo2max.get().getVo2maxEstimate();
            double previousVdot = vdotHistoryService.getEffectiveVdot(user).orElse(0.0);

            if (newVdot != previousVdot) {
                vdotHistoryService.recordCalculation(
                        user, previousVdot, newVdot, null, latestVo2max.get().getId());
                vdotUpdated = true;
            }
        }

        // 5. Run adjustment engine
        AdjustmentDecision adjustment = planAdjustmentEngine.evaluate(user);

        // 6. Update lastSyncedAt
        user.setLastSyncedAt(Instant.now());
        userRepository.save(user);

        // 7. Return summary
        return new HealthSyncResponse(
                workoutsSaved, workoutsSkipped, workoutsMatched, snapshotsSaved, vdotUpdated,
                adjustment.type().name());
    }
}
