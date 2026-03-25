package com.runplanner.goalrace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.GoalRaceResponse;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import com.runplanner.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalRaceController.class)
@Import(com.runplanner.config.SecurityConfig.class)
class GoalRaceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean GoalRaceService goalRaceService;
    @MockBean UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("u@x.com").passwordHash("h").build();
    }

    @Test
    void createGoalRace_returns201() throws Exception {
        var user = user();
        var response = new GoalRaceResponse(UUID.randomUUID(), 21097, "Half Marathon",
            LocalDate.of(2026, 10, 1), 6600, GoalRaceStatus.ACTIVE);
        when(goalRaceService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/goal-races")
                .with(SecurityMockMvcRequestPostProcessors.user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateGoalRaceRequest(21097, "Half Marathon",
                        LocalDate.of(2026, 10, 1), 6600))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.distanceLabel").value("Half Marathon"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createGoalRace_returns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/goal-races")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listGoalRaces_returnsAll() throws Exception {
        var user = user();
        var race = new GoalRaceResponse(UUID.randomUUID(), 5000, "5K",
            LocalDate.of(2026, 6, 1), null, GoalRaceStatus.ACTIVE);
        when(goalRaceService.findAll(any())).thenReturn(List.of(race));

        mockMvc.perform(get("/api/v1/goal-races")
                .with(SecurityMockMvcRequestPostProcessors.user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].distanceLabel").value("5K"));
    }

    @Test
    void updateGoalRace_returns200() throws Exception {
        var user = user();
        var raceId = UUID.randomUUID();
        var updated = new GoalRaceResponse(raceId, 5000, "5K",
            LocalDate.of(2026, 9, 1), null, GoalRaceStatus.ARCHIVED);
        when(goalRaceService.update(any(), any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/goal-races/" + raceId)
                .with(SecurityMockMvcRequestPostProcessors.user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateGoalRaceRequest(LocalDate.of(2026, 9, 1), null, GoalRaceStatus.ARCHIVED))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
}
