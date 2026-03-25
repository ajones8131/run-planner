package com.runplanner.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runplanner.TestSecurityConfig;
import com.runplanner.user.dto.UpdateProfileRequest;
import com.runplanner.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    private User authenticatedUser() {
        return User.builder().id(UUID.randomUUID()).email("u@x.com").passwordHash("h").build();
    }

    @Test
    void getProfile_returns200WithUserData() throws Exception {
        var user = authenticatedUser();
        var response = new UserResponse(user.getId(), user.getEmail(), "Alice", null, 185, Units.METRIC);
        when(userService.getProfile(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                .with(SecurityMockMvcRequestPostProcessors.user(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("u@x.com"))
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getProfile_returns401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_returns200WithUpdatedData() throws Exception {
        var user = authenticatedUser();
        var updated = new UserResponse(user.getId(), user.getEmail(), "Bob", null, 190, Units.IMPERIAL);
        when(userService.updateProfile(any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/users/me")
                .with(SecurityMockMvcRequestPostProcessors.user(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UpdateProfileRequest("Bob", null, 190, Units.IMPERIAL))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bob"))
            .andExpect(jsonPath("$.maxHr").value(190));
    }
}
