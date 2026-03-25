package com.runplanner.user.dto;

import com.runplanner.user.Units;
import com.runplanner.user.User;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String name,
    LocalDate dateOfBirth,
    Integer maxHr,
    Units preferredUnits
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(),
            user.getDateOfBirth(), user.getMaxHr(), user.getPreferredUnits());
    }
}
