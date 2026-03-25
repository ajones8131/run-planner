package com.runplanner.plan;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceRepository;
import com.runplanner.goalrace.GoalRaceStatus;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import com.runplanner.vdot.VdotHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {

    @Mock private TrainingPlanRepository trainingPlanRepository;
    @Mock private PlannedWorkoutRepository plannedWorkoutRepository;
    @Mock private TrainingPlanGenerator trainingPlanGenerator;
    @Mock private GoalRaceRepository goalRaceRepository;
    @Mock private VdotHistoryService vdotHistoryService;
    @Mock private Clock clock;

    @InjectMocks private TrainingPlanService service;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").build();
    }

    private GoalRace goalRace(User user) {
        return GoalRace.builder()
                .id(UUID.randomUUID())
                .user(user)
                .distanceMeters(21097)
                .distanceLabel("Half Marathon")
                .raceDate(LocalDate.of(2026, 6, 1))
                .status(GoalRaceStatus.ACTIVE)
                .build();
    }

    // --- generate ---

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-03-25T12:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void generate_happyPath_createsPlanAndWorkouts() {
        stubClock();
        User user = user();
        GoalRace race = goalRace(user);
        when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
        when(trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)).thenReturn(false);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.of(50.0));
        when(trainingPlanRepository.save(any())).thenAnswer(inv -> {
            TrainingPlan p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(trainingPlanGenerator.generate(eq(50.0), eq(21097), eq(race.getRaceDate()), any()))
                .thenReturn(List.of(PlannedWorkout.builder().weekNumber(1).dayOfWeek(1)
                        .scheduledDate(LocalDate.now()).workoutType(WorkoutType.REST)
                        .targetDistanceMeters(0).originalScheduledDate(LocalDate.now()).build()));
        when(plannedWorkoutRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        TrainingPlanResponse result = service.generate(user, race.getId());

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("ACTIVE");
        verify(trainingPlanRepository).save(any());
        verify(plannedWorkoutRepository).saveAll(anyList());
    }

    @Test
    void generate_activePlanExists_throws409() {
        User user = user();
        GoalRace race = goalRace(user);
        when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
        when(trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.generate(user, race.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("active plan");
    }

    @Test
    void generate_noVdot_throws400() {
        User user = user();
        GoalRace race = goalRace(user);
        when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
        when(trainingPlanRepository.existsByUserAndStatus(user, TrainingPlanStatus.ACTIVE)).thenReturn(false);
        when(vdotHistoryService.getEffectiveVdot(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(user, race.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("VDOT");
    }

    @Test
    void generate_goalRaceNotFound_throws404() {
        User user = user();
        UUID raceId = UUID.randomUUID();
        when(goalRaceRepository.findByIdAndUser(raceId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(user, raceId))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- findActive ---

    @Test
    void findActive_withActivePlan_returnsPlanWithWorkouts() {
        User user = user();
        TrainingPlan plan = TrainingPlan.builder().id(UUID.randomUUID()).user(user)
                .goalRace(goalRace(user)).startDate(LocalDate.now()).endDate(LocalDate.now().plusWeeks(12))
                .status(TrainingPlanStatus.ACTIVE).build();
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.of(plan));
        when(plannedWorkoutRepository.findAllByTrainingPlanOrderByScheduledDate(plan))
                .thenReturn(List.of());

        TrainingPlanResponse result = service.findActive(user);

        assertThat(result).isNotNull();
        assertThat(result.workouts()).isNotNull();
    }

    @Test
    void findActive_noActivePlan_throws404() {
        User user = user();
        when(trainingPlanRepository.findByUserAndStatus(user, TrainingPlanStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findActive(user))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- findById ---

    @Test
    void findById_foreignPlan_throws404() {
        User user = user();
        UUID planId = UUID.randomUUID();
        when(trainingPlanRepository.findByIdAndUser(planId, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(user, planId))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- archive ---

    @Test
    void archive_activePlan_setsStatusArchived() {
        User user = user();
        TrainingPlan plan = TrainingPlan.builder().id(UUID.randomUUID()).user(user)
                .goalRace(goalRace(user)).status(TrainingPlanStatus.ACTIVE).build();
        when(trainingPlanRepository.findByIdAndUser(plan.getId(), user)).thenReturn(Optional.of(plan));
        when(trainingPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.archive(user, plan.getId());

        ArgumentCaptor<TrainingPlan> captor = ArgumentCaptor.forClass(TrainingPlan.class);
        verify(trainingPlanRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TrainingPlanStatus.ARCHIVED);
    }

    @Test
    void archive_nonActivePlan_throws400() {
        User user = user();
        TrainingPlan plan = TrainingPlan.builder().id(UUID.randomUUID()).user(user)
                .goalRace(goalRace(user)).status(TrainingPlanStatus.ARCHIVED).build();
        when(trainingPlanRepository.findByIdAndUser(plan.getId(), user)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.archive(user, plan.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ACTIVE");
    }
}
