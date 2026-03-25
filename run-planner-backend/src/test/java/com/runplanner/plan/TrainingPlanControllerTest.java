package com.runplanner.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.config.SecurityConfig;
import com.runplanner.plan.dto.CreatePlanRequest;
import com.runplanner.plan.dto.PlannedWorkoutResponse;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainingPlanController.class)
@Import(SecurityConfig.class)
class TrainingPlanControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean TrainingPlanService trainingPlanService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").build();
    }

    private TrainingPlanResponse samplePlanResponse() {
        return new TrainingPlanResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 1),
                "ACTIVE", 1, Instant.now(), List.of());
    }

    // --- POST /plans ---

    @Test
    void createPlan_returns201() throws Exception {
        User u = testUser();
        UUID raceId = UUID.randomUUID();
        when(trainingPlanService.generate(any(), eq(raceId))).thenReturn(samplePlanResponse());

        mockMvc.perform(post("/api/v1/plans")
                        .with(user(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlanRequest(raceId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createPlan_activePlanExists_returns409() throws Exception {
        User u = testUser();
        UUID raceId = UUID.randomUUID();
        when(trainingPlanService.generate(any(), eq(raceId)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/plans")
                        .with(user(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlanRequest(raceId))))
                .andExpect(status().isConflict());
    }

    @Test
    void createPlan_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /plans ---

    @Test
    void listPlans_returnsAll() throws Exception {
        User u = testUser();
        when(trainingPlanService.findAll(any())).thenReturn(List.of(samplePlanResponse()));

        mockMvc.perform(get("/api/v1/plans").with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // --- GET /plans/active ---

    @Test
    void getActivePlan_returnsPlan() throws Exception {
        User u = testUser();
        when(trainingPlanService.findActive(any())).thenReturn(samplePlanResponse());

        mockMvc.perform(get("/api/v1/plans/active").with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getActivePlan_noPlan_returns404() throws Exception {
        User u = testUser();
        when(trainingPlanService.findActive(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/plans/active").with(user(u)))
                .andExpect(status().isNotFound());
    }

    // --- GET /plans/{id} ---

    @Test
    void getPlanById_returnsPlan() throws Exception {
        User u = testUser();
        TrainingPlanResponse response = samplePlanResponse();
        when(trainingPlanService.findById(any(), eq(response.id()))).thenReturn(response);

        mockMvc.perform(get("/api/v1/plans/" + response.id()).with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(1));
    }

    // --- GET /plans/{id}/workouts ---

    @Test
    void getPlannedWorkouts_returnsWorkouts() throws Exception {
        User u = testUser();
        UUID planId = UUID.randomUUID();
        PlannedWorkoutResponse workout = new PlannedWorkoutResponse(
                UUID.randomUUID(), 1, 2, LocalDate.of(2026, 1, 6),
                "EASY", 8000.0, 5.0, 6.0, null, null, 1);
        when(trainingPlanService.findPlannedWorkouts(any(), eq(planId), isNull(), isNull()))
                .thenReturn(List.of(workout));

        mockMvc.perform(get("/api/v1/plans/" + planId + "/workouts").with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workoutType").value("EASY"));
    }

    @Test
    void getPlannedWorkouts_withDateFilters() throws Exception {
        User u = testUser();
        UUID planId = UUID.randomUUID();
        when(trainingPlanService.findPlannedWorkouts(any(), eq(planId),
                eq(LocalDate.of(2026, 1, 5)), eq(LocalDate.of(2026, 1, 11))))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/plans/" + planId + "/workouts")
                        .param("from", "2026-01-05")
                        .param("to", "2026-01-11")
                        .with(user(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // --- DELETE /plans/{id} ---

    @Test
    void archivePlan_returns204() throws Exception {
        User u = testUser();
        UUID planId = UUID.randomUUID();
        doNothing().when(trainingPlanService).archive(any(), eq(planId));

        mockMvc.perform(delete("/api/v1/plans/" + planId).with(user(u)))
                .andExpect(status().isNoContent());
    }

    @Test
    void archivePlan_nonActive_returns400() throws Exception {
        User u = testUser();
        UUID planId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
                .when(trainingPlanService).archive(any(), eq(planId));

        mockMvc.perform(delete("/api/v1/plans/" + planId).with(user(u)))
                .andExpect(status().isBadRequest());
    }

    // --- Auth ---

    @Test
    void allEndpoints_unauthenticated_return401() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/plans")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/plans/active")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/plans/" + id)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/plans/" + id + "/workouts")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/plans/" + id)).andExpect(status().isUnauthorized());
    }
}
