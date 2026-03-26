package com.runplanner.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.config.SecurityConfig;
import com.runplanner.health.dto.HealthSyncRequest;
import com.runplanner.health.dto.HealthSyncResponse;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthSyncController.class)
@Import(SecurityConfig.class)
class HealthSyncControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean HealthSyncService healthSyncService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").passwordHash("h").build();
    }

    @Test
    void sync_returns200WithSummary() throws Exception {
        User u = testUser();
        when(healthSyncService.sync(any(), any()))
                .thenReturn(new HealthSyncResponse(2, 1, 1, 1, true, "NONE"));

        mockMvc.perform(post("/api/v1/health/sync")
                        .with(user(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HealthSyncRequest(List.of(), List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutsSaved").value(2))
                .andExpect(jsonPath("$.workoutsSkipped").value(1))
                .andExpect(jsonPath("$.workoutsMatched").value(1))
                .andExpect(jsonPath("$.snapshotsSaved").value(1))
                .andExpect(jsonPath("$.vdotUpdated").value(true));
    }

    @Test
    void sync_emptyBody_returns200() throws Exception {
        User u = testUser();
        when(healthSyncService.sync(any(), any()))
                .thenReturn(new HealthSyncResponse(0, 0, 0, 0, false, "NONE"));

        mockMvc.perform(post("/api/v1/health/sync")
                        .with(user(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutsSaved").value(0));
    }

    @Test
    void sync_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/health/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
