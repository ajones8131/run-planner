package com.runplanner.workout;

import com.runplanner.config.SecurityConfig;
import com.runplanner.user.User;
import com.runplanner.workout.dto.WorkoutResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkoutController.class)
@Import(SecurityConfig.class)
class WorkoutControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean WorkoutService workoutService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("hashed")
                .build();
    }

    private WorkoutResponse sampleResponse() {
        return new WorkoutResponse(
                UUID.randomUUID(),
                Instant.parse("2026-03-20T08:00:00Z"),
                5000.0,
                1200,
                165,
                180,
                50.0,
                "APPLE_HEALTH",
                "abc-123"
        );
    }

    @Test
    void listWorkouts_authenticated_returnsWorkouts() throws Exception {
        User u = testUser();
        WorkoutResponse response = sampleResponse();
        when(workoutService.findAll(any(), isNull())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/workouts").with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceMeters").value(5000.0))
                .andExpect(jsonPath("$[0].durationSeconds").value(1200))
                .andExpect(jsonPath("$[0].source").value("APPLE_HEALTH"));
    }

    @Test
    void listWorkouts_withSinceParam_passesInstantToService() throws Exception {
        User u = testUser();
        when(workoutService.findAll(any(), eq(Instant.parse("2026-03-19T00:00:00Z"))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/workouts")
                        .param("since", "2026-03-19T00:00:00Z")
                        .with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listWorkouts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/workouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWorkout_authenticated_returnsWorkout() throws Exception {
        User u = testUser();
        WorkoutResponse response = sampleResponse();
        when(workoutService.findById(any(), eq(response.id()))).thenReturn(response);

        mockMvc.perform(get("/api/v1/workouts/" + response.id()).with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeters").value(5000.0))
                .andExpect(jsonPath("$.sourceId").value("abc-123"));
    }

    @Test
    void getWorkout_notFound_returns404() throws Exception {
        User u = testUser();
        UUID id = UUID.randomUUID();
        when(workoutService.findById(any(), eq(id)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/workouts/" + id).with(user(u)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWorkout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/workouts/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
