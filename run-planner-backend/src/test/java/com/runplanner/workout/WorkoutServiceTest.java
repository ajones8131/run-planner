package com.runplanner.workout;

import com.runplanner.user.User;
import com.runplanner.vdot.VdotRecalculationService;
import com.runplanner.workout.dto.WorkoutResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock private WorkoutRepository workoutRepository;
    @Mock private VdotRecalculationService vdotRecalculationService;

    @InjectMocks private WorkoutService workoutService;

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("hashed")
                .build();
    }

    private Workout workout(User user) {
        return Workout.builder()
                .id(UUID.randomUUID())
                .user(user)
                .source("APPLE_HEALTH")
                .sourceId("abc-123")
                .startedAt(Instant.parse("2026-03-20T08:00:00Z"))
                .distanceMeters(5000.0)
                .durationSeconds(1200)
                .avgHr(165)
                .maxHr(180)
                .build();
    }

    @Test
    void save_persistsAndTriggersRecalculation() {
        User user = user();
        Workout w = workout(user);
        when(workoutRepository.save(w)).thenReturn(w);
        when(vdotRecalculationService.evaluate(w)).thenReturn(Optional.empty());

        Workout result = workoutService.save(w);

        assertThat(result).isEqualTo(w);
        verify(workoutRepository).save(w);
        verify(vdotRecalculationService).evaluate(w);
    }

    @Test
    void findAll_withSince_usesFilteredQuery() {
        User user = user();
        Workout w = workout(user);
        Instant since = Instant.parse("2026-03-19T00:00:00Z");
        when(workoutRepository.findAllByUserAndStartedAtAfterOrderByStartedAtDesc(user, since))
                .thenReturn(List.of(w));

        List<WorkoutResponse> result = workoutService.findAll(user, since);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(w.getId());
        verify(workoutRepository).findAllByUserAndStartedAtAfterOrderByStartedAtDesc(user, since);
    }

    @Test
    void findAll_withoutSince_usesUnfilteredQuery() {
        User user = user();
        Workout w = workout(user);
        when(workoutRepository.findAllByUserOrderByStartedAtDesc(user))
                .thenReturn(List.of(w));

        List<WorkoutResponse> result = workoutService.findAll(user, null);

        assertThat(result).hasSize(1);
        verify(workoutRepository).findAllByUserOrderByStartedAtDesc(user);
    }

    @Test
    void findById_found_returnsResponse() {
        User user = user();
        Workout w = workout(user);
        when(workoutRepository.findByIdAndUser(w.getId(), user)).thenReturn(Optional.of(w));

        WorkoutResponse result = workoutService.findById(user, w.getId());

        assertThat(result.id()).isEqualTo(w.getId());
        assertThat(result.distanceMeters()).isEqualTo(5000.0);
    }

    @Test
    void findById_notFound_throws404() {
        User user = user();
        UUID id = UUID.randomUUID();
        when(workoutRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutService.findById(user, id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workout not found");
    }

    @Test
    void existsBySourceAndSourceId_delegatesToRepository() {
        when(workoutRepository.existsBySourceAndSourceId("APPLE_HEALTH", "abc-123"))
                .thenReturn(true);

        boolean result = workoutService.existsBySourceAndSourceId("APPLE_HEALTH", "abc-123");

        assertThat(result).isTrue();
        verify(workoutRepository).existsBySourceAndSourceId("APPLE_HEALTH", "abc-123");
    }
}
