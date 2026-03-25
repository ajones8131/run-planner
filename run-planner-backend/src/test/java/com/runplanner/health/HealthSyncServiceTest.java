package com.runplanner.health;

import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncRequest.HealthSnapshotSyncItem;
import com.runplanner.health.dto.HealthSyncRequest.WorkoutSyncItem;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.match.WorkoutMatch;
import com.runplanner.match.WorkoutMatcher;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import com.runplanner.vdot.VdotHistory;
import com.runplanner.vdot.VdotHistoryService;
import com.runplanner.workout.Workout;
import com.runplanner.workout.WorkoutService;
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
class HealthSyncServiceTest {

    @Mock private WorkoutService workoutService;
    @Mock private WorkoutMatcher workoutMatcher;
    @Mock private HealthSnapshotRepository healthSnapshotRepository;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private UserRepository userRepository;

    @InjectMocks private HealthSyncService service;

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("h")
                .build();
    }

    private WorkoutSyncItem workoutItem(String sourceId) {
        return new WorkoutSyncItem(
                "APPLE_HEALTH", sourceId, Instant.parse("2026-03-25T08:00:00Z"),
                10000.0, 3000, 155, 172, 45.0);
    }

    private HealthSnapshotSyncItem snapshotItem(Double vo2max) {
        return new HealthSnapshotSyncItem(vo2max, 52, Instant.parse("2026-03-25T06:00:00Z"));
    }

    @Test
    void sync_workoutsOnly_savesNonDuplicatesAndSkipsDuplicates() {
        User user = user();
        when(workoutService.existsBySourceAndSourceId("APPLE_HEALTH", "new-1")).thenReturn(false);
        when(workoutService.existsBySourceAndSourceId("APPLE_HEALTH", "dup-1")).thenReturn(true);
        when(workoutService.save(any())).thenAnswer(inv -> {
            Workout w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(workoutMatcher.match(any())).thenReturn(Optional.empty());

        HealthSyncRequest request = new HealthSyncRequest(
                List.of(workoutItem("new-1"), workoutItem("dup-1")), null);

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isEqualTo(1);
        assertThat(response.workoutsSkipped()).isEqualTo(1);
        verify(workoutService, times(1)).save(any());
    }

    @Test
    void sync_matching_newWorkoutsPassedToMatcher() {
        User user = user();
        when(workoutService.existsBySourceAndSourceId(any(), any())).thenReturn(false);
        when(workoutService.save(any())).thenAnswer(inv -> {
            Workout w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(workoutMatcher.match(any())).thenReturn(
                Optional.of(WorkoutMatch.builder().complianceScore(0.9).build()));

        HealthSyncRequest request = new HealthSyncRequest(
                List.of(workoutItem("w-1")), null);

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsMatched()).isEqualTo(1);
        verify(workoutMatcher).match(any());
    }

    @Test
    void sync_snapshotsOnly_savesAll() {
        User user = user();
        when(healthSnapshotRepository.save(any())).thenAnswer(inv -> {
            HealthSnapshot s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.empty());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5), snapshotItem(null)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.snapshotsSaved()).isEqualTo(2);
        verify(healthSnapshotRepository, times(2)).save(any());
    }

    @Test
    void sync_vo2maxSnapshot_triggersVdotRecording() {
        User user = user();
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(48.5)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(45.0));
        when(vdotHistoryService.recordCalculation(eq(user), eq(45.0), eq(48.5), isNull(), eq(snapshot.getId())))
                .thenReturn(VdotHistory.builder().newVdot(48.5).build());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isTrue();
        verify(vdotHistoryService).recordCalculation(user, 45.0, 48.5, null, snapshot.getId());
    }

    @Test
    void sync_vo2maxEqualsCurrentVdot_skipsRecording() {
        User user = user();
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(48.5)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.5));

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isFalse();
        verify(vdotHistoryService, never()).recordCalculation(any(), anyDouble(), anyDouble(), any(), any());
    }

    @Test
    void sync_noVo2maxInSnapshots_skipsVdotRecording() {
        User user = user();
        when(healthSnapshotRepository.save(any())).thenAnswer(inv -> {
            HealthSnapshot s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.empty());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(null)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isFalse();
        verify(vdotHistoryService, never()).recordCalculation(any(), anyDouble(), anyDouble(), any(), any());
    }

    @Test
    void sync_vo2maxWithNoExistingVdot_recordsWithZeroPrevious() {
        User user = user();
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(48.5)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());
        when(vdotHistoryService.recordCalculation(eq(user), eq(0.0), eq(48.5), isNull(), eq(snapshot.getId())))
                .thenReturn(VdotHistory.builder().newVdot(48.5).build());

        HealthSyncRequest request = new HealthSyncRequest(
                null, List.of(snapshotItem(48.5)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.vdotUpdated()).isTrue();
        verify(vdotHistoryService).recordCalculation(user, 0.0, 48.5, null, snapshot.getId());
    }

    @Test
    void sync_updatesLastSyncedAt() {
        User user = user();

        HealthSyncRequest request = new HealthSyncRequest(null, null);
        service.sync(user, request);

        assertThat(user.getLastSyncedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void sync_emptyRequest_returnsZerosAndUpdatesLastSynced() {
        User user = user();

        HealthSyncRequest request = new HealthSyncRequest(List.of(), List.of());
        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isZero();
        assertThat(response.workoutsSkipped()).isZero();
        assertThat(response.workoutsMatched()).isZero();
        assertThat(response.snapshotsSaved()).isZero();
        assertThat(response.vdotUpdated()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void sync_nullLists_treatedAsEmpty() {
        User user = user();

        HealthSyncRequest request = new HealthSyncRequest(null, null);
        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isZero();
        assertThat(response.snapshotsSaved()).isZero();
        verify(userRepository).save(user);
    }

    @Test
    void sync_bothWorkoutsAndSnapshots_fullFlow() {
        User user = user();
        when(workoutService.existsBySourceAndSourceId(any(), any())).thenReturn(false);
        when(workoutService.save(any())).thenAnswer(inv -> {
            Workout w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(workoutMatcher.match(any())).thenReturn(Optional.empty());
        HealthSnapshot snapshot = HealthSnapshot.builder()
                .id(UUID.randomUUID()).user(user).vo2maxEstimate(50.0)
                .recordedAt(Instant.now()).build();
        when(healthSnapshotRepository.save(any())).thenReturn(snapshot);
        when(healthSnapshotRepository.findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(user))
                .thenReturn(Optional.of(snapshot));
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(48.0));
        when(vdotHistoryService.recordCalculation(any(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(VdotHistory.builder().newVdot(50.0).build());

        HealthSyncRequest request = new HealthSyncRequest(
                List.of(workoutItem("w-1")),
                List.of(snapshotItem(50.0)));

        HealthSyncResponse response = service.sync(user, request);

        assertThat(response.workoutsSaved()).isEqualTo(1);
        assertThat(response.snapshotsSaved()).isEqualTo(1);
        assertThat(response.vdotUpdated()).isTrue();
        verify(userRepository).save(user);
    }
}
