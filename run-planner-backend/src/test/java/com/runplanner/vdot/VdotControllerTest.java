package com.runplanner.vdot;

import com.runplanner.config.SecurityConfig;
import com.runplanner.user.User;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VdotController.class)
@Import(SecurityConfig.class)
class VdotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean VdotHistoryService vdotHistoryService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("hashed")
                .build();
    }

    // --- GET /history ---

    @Test
    void getHistory_authenticated_returnsHistoryList() throws Exception {
        User user = testUser();
        UUID workoutId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(UUID.randomUUID())
                .user(user)
                .triggeringWorkoutId(workoutId)
                .previousVdot(50.0)
                .newVdot(52.0)
                .calculatedAt(Instant.now())
                .flagged(false)
                .accepted(true)
                .build();
        when(vdotHistoryService.getHistory(any())).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/vdot/history").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousVdot").value(50.0))
                .andExpect(jsonPath("$[0].newVdot").value(52.0))
                .andExpect(jsonPath("$[0].flagged").value(false))
                .andExpect(jsonPath("$[0].accepted").value(true));
    }

    @Test
    void getHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vdot/history"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /history/{id}/accept ---

    @Test
    void acceptFlagged_authenticated_returns200() throws Exception {
        User user = testUser();
        UUID historyId = UUID.randomUUID();
        doNothing().when(vdotHistoryService).acceptFlagged(any(), eq(historyId));

        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/accept")
                        .with(user(user)))
                .andExpect(status().isOk());

        verify(vdotHistoryService).acceptFlagged(any(), eq(historyId));
    }

    @Test
    void acceptFlagged_unauthenticated_returns401() throws Exception {
        UUID historyId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/accept"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /history/{id}/dismiss ---

    @Test
    void dismissFlagged_authenticated_returns200() throws Exception {
        User user = testUser();
        UUID historyId = UUID.randomUUID();
        doNothing().when(vdotHistoryService).dismissFlagged(any(), eq(historyId));

        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/dismiss")
                        .with(user(user)))
                .andExpect(status().isOk());

        verify(vdotHistoryService).dismissFlagged(any(), eq(historyId));
    }

    @Test
    void dismissFlagged_unauthenticated_returns401() throws Exception {
        UUID historyId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/dismiss"))
                .andExpect(status().isUnauthorized());
    }
}
