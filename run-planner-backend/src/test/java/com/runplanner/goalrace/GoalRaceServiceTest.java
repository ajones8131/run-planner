package com.runplanner.goalrace;

import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalRaceServiceTest {

    @Mock GoalRaceRepository goalRaceRepository;
    @InjectMocks GoalRaceService goalRaceService;

    private User user() {
        return User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void create_savesAndReturnsGoalRace() {
        var user = user();
        var request = new CreateGoalRaceRequest(21097, "Half Marathon",
            LocalDate.of(2026, 10, 1), 6600);
        when(goalRaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = goalRaceService.create(user, request);

        assertThat(response.distanceMeters()).isEqualTo(21097);
        assertThat(response.distanceLabel()).isEqualTo("Half Marathon");
        assertThat(response.status()).isEqualTo(GoalRaceStatus.ACTIVE);
        verify(goalRaceRepository).save(any());
    }

    @Test
    void findAll_returnsAllRacesForUser() {
        var user = user();
        var race = GoalRace.builder().id(UUID.randomUUID()).user(user)
            .distanceMeters(5000).distanceLabel("5K")
            .raceDate(LocalDate.now()).build();
        when(goalRaceRepository.findAllByUserOrderByRaceDateDesc(user)).thenReturn(List.of(race));

        var responses = goalRaceService.findAll(user);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).distanceLabel()).isEqualTo("5K");
    }

    @Test
    void update_modifiesMutableFields() {
        var user = user();
        var race = GoalRace.builder().id(UUID.randomUUID()).user(user)
            .distanceMeters(5000).distanceLabel("5K")
            .raceDate(LocalDate.of(2026, 6, 1))
            .status(GoalRaceStatus.ACTIVE).build();
        when(goalRaceRepository.findByIdAndUser(race.getId(), user)).thenReturn(Optional.of(race));
        when(goalRaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updated = goalRaceService.update(user, race.getId(),
            new UpdateGoalRaceRequest(LocalDate.of(2026, 9, 1), 1200, GoalRaceStatus.ARCHIVED));

        assertThat(updated.raceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(updated.status()).isEqualTo(GoalRaceStatus.ARCHIVED);
    }

    @Test
    void update_throws404WhenNotFound() {
        var user = user();
        when(goalRaceRepository.findByIdAndUser(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> goalRaceService.update(user, UUID.randomUUID(),
            new UpdateGoalRaceRequest(null, null, null)))
            .isInstanceOf(ResponseStatusException.class);
    }
}
